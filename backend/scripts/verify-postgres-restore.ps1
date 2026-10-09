param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [Parameter(Mandatory = $true)]
    [string]$RestoreDatabaseUrl
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $BackupFile -PathType Leaf)) {
    throw "Backup file not found: $BackupFile"
}

try {
    $target = [Uri]$RestoreDatabaseUrl
} catch {
    throw 'RestoreDatabaseUrl must be a PostgreSQL connection URI (postgresql://...).'
}

if ($target.Scheme -notin @('postgres', 'postgresql') -or [string]::IsNullOrWhiteSpace($target.Host)) {
    throw 'RestoreDatabaseUrl must be a PostgreSQL URI with a host.'
}

$databaseName = [Uri]::UnescapeDataString($target.AbsolutePath.Trim('/'))
if ([string]::IsNullOrWhiteSpace($databaseName)) {
    throw 'RestoreDatabaseUrl must include a database name.'
}
if ($databaseName -notmatch '(?i)(restore|scratch|drill|test)') {
    throw "Refusing restore target '$databaseName'. Use a separate empty database whose name includes restore, scratch, drill, or test."
}

$pgRestore = Get-Command pg_restore -ErrorAction SilentlyContinue
$psql = Get-Command psql -ErrorAction SilentlyContinue
if (-not $pgRestore -or -not $psql) {
    throw 'Install PostgreSQL 16 client tools so pg_restore and psql are on PATH.'
}

$connectionVariables = @('PGHOST', 'PGPORT', 'PGUSER', 'PGPASSWORD', 'PGSSLPASSWORD', 'PGSSLMODE')
$previousConnectionEnvironment = @{}
foreach ($name in $connectionVariables) {
    $previousConnectionEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$targetUser = ''
$targetPassword = $null
if (-not [string]::IsNullOrEmpty($target.UserInfo)) {
    $userInfo = [Uri]::UnescapeDataString($target.UserInfo)
    $separator = $userInfo.IndexOf(':')
    if ($separator -ge 0) {
        $targetUser = $userInfo.Substring(0, $separator)
        $targetPassword = $userInfo.Substring($separator + 1)
    } else {
        $targetUser = $userInfo
    }
}

# libpq accepts passwords from PGPASSWORD. Remove them from the URI passed to
# psql/pg_restore so process listings and native-client diagnostics cannot leak
# credentials. Support the libpq `password` query parameter as well.
$uriBuilder = [UriBuilder]::new($target)
$safeQueryParts = @()
$targetSslMode = 'require'
foreach ($part in $target.Query.TrimStart('?').Split('&')) {
    if ([string]::IsNullOrEmpty($part)) { continue }
    $pair = $part.Split('=', 2)
    $key = [Uri]::UnescapeDataString($pair[0])
    if ($key -ieq 'password') {
        if ($pair.Length -gt 1) { $targetPassword = [Uri]::UnescapeDataString($pair[1].Replace('+', ' ')) }
    } elseif ($key -ieq 'sslpassword') {
        if ($pair.Length -gt 1) { $targetSslPassword = [Uri]::UnescapeDataString($pair[1].Replace('+', ' ')) }
    } elseif ($key -ieq 'sslmode') {
        if ($pair.Length -gt 1) { $targetSslMode = [Uri]::UnescapeDataString($pair[1].Replace('+', ' ')) }
        if ($targetSslMode -notin @('require', 'verify-ca', 'verify-full')) {
            throw 'RestoreDatabaseUrl must require TLS (sslmode=require, verify-ca, or verify-full).'
        }
        $safeQueryParts += $part
    } else {
        $safeQueryParts += $part
    }
}
$uriBuilder.Password = ''
$uriBuilder.Query = $safeQueryParts -join '&'
$safeTargetUrl = $uriBuilder.Uri.AbsoluteUri

try {
    $env:PGHOST = $target.Host
    if ($target.Port -gt 0) { $env:PGPORT = "$($target.Port)" } else { Remove-Item Env:PGPORT -ErrorAction SilentlyContinue }
    if (-not [string]::IsNullOrEmpty($targetUser)) { $env:PGUSER = $targetUser }
    if ($null -ne $targetPassword) { $env:PGPASSWORD = $targetPassword }
    if ($null -ne $targetSslPassword) { $env:PGSSLPASSWORD = $targetSslPassword }
    $env:PGSSLMODE = $targetSslMode

    # Never erase a target database. Restore only into a new, empty scratch database.
    $existingTables = & $psql.Source $safeTargetUrl -X -A -t -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM pg_catalog.pg_tables WHERE schemaname = 'public';"
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the restore target database.' }
    if ([int]($existingTables.Trim()) -ne 0) {
        throw "Restore target '$databaseName' is not empty. Create a new scratch database and retry."
    }

    & $pgRestore.Source --no-owner --no-privileges --exit-on-error --dbname=$safeTargetUrl $BackupFile
    if ($LASTEXITCODE -ne 0) { throw 'pg_restore failed.' }

    $requiredTables = @('agencies', 'app_users', 'clients', 'projects', 'tasks', 'proposals', 'contracts', 'flyway_schema_history')
    $tableQuery = "SELECT string_agg(required.name, ',' ORDER BY required.name) FROM unnest(ARRAY['$($requiredTables -join "','")']::text[]) required(name) LEFT JOIN pg_catalog.pg_tables actual ON actual.schemaname = 'public' AND actual.tablename = required.name WHERE actual.tablename IS NULL;"
    $missingTables = & $psql.Source $safeTargetUrl -X -A -t -v ON_ERROR_STOP=1 -c $tableQuery
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify restored schema.' }
    if (-not [string]::IsNullOrWhiteSpace($missingTables)) {
        throw "Restore completed, but required tables are missing: $($missingTables.Trim())"
    }

    $missingColumns = & $psql.Source $safeTargetUrl -X -A -t -v ON_ERROR_STOP=1 -c "SELECT string_agg(required.table_name || '.' || required.column_name, ',' ORDER BY required.table_name, required.column_name) FROM (VALUES ('proposals', 'client_email')) AS required(table_name, column_name) LEFT JOIN information_schema.columns actual ON actual.table_schema = 'public' AND actual.table_name = required.table_name AND actual.column_name = required.column_name WHERE actual.column_name IS NULL;"
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify restored application columns.' }
    if (-not [string]::IsNullOrWhiteSpace($missingColumns)) {
        throw "Restore completed, but required columns are missing: $($missingColumns.Trim())"
    }

    $migrationCount = & $psql.Source $safeTargetUrl -X -A -t -v ON_ERROR_STOP=1 -c 'SELECT count(*) FROM public.flyway_schema_history WHERE success = true;'
    if ($LASTEXITCODE -ne 0 -or [int]($migrationCount.Trim()) -lt 1) {
        throw 'Restore completed, but no successful Flyway migration record was found.'
    }

    $latestMigration = & $psql.Source $safeTargetUrl -X -A -t -v ON_ERROR_STOP=1 -c 'SELECT version FROM public.flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1;'
    if ($LASTEXITCODE -ne 0 -or $latestMigration.Trim() -ne '2') {
        throw 'Restore completed, but the database is not at the required Flyway V2 schema version.'
    }

    Write-Output "Restore verification passed for '$databaseName': required tables, V2 columns, and current Flyway history are present."
} finally {
    foreach ($name in $connectionVariables) {
        [Environment]::SetEnvironmentVariable($name, $previousConnectionEnvironment[$name], 'Process')
    }
}
