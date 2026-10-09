param([int]$Port = 0)

$ErrorActionPreference = 'Stop'
if ($Port -lt 0 -or $Port -gt 65535) {
    throw 'Port must be between 1 and 65535, or omitted to use backend/.env / the default port 8080.'
}
Set-Location -LiteralPath $PSScriptRoot

$envPath = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
    throw 'backend/.env was not found. Copy backend/.env.example to backend/.env and fill in the local values first.'
}

foreach ($line in Get-Content -LiteralPath $envPath) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        $name = $matches[1]
        $value = $matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or $env:JWT_SECRET.Length -lt 32) {
    throw 'JWT_SECRET is missing or too short in backend/.env. Set a private secret of at least 32 characters.'
}

# Local development always uses disposable H2, even if backend/.env points at Neon.
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SPRING_DATASOURCE_URL = 'jdbc:h2:mem:freelancedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL'
$env:SPRING_DATASOURCE_DRIVER = 'org.h2.Driver'
$env:SPRING_DATASOURCE_USERNAME = 'sa'
$env:SPRING_DATASOURCE_PASSWORD = ''
$env:SERVER_PORT = if ($Port -gt 0) { [string]$Port } elseif ([string]::IsNullOrWhiteSpace($env:SERVER_PORT)) { '8080' } else { $env:SERVER_PORT }
$env:APP_EMAIL_DELIVERY_MODE = 'console'
$env:APP_FRONTEND_BASE_URL = 'http://localhost:5173'
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173,http://localhost:5174,http://localhost:5175,http://localhost:3000'
$env:RESEND_API_KEY = ''
$env:RESEND_FROM = 'Local Development <local@example.test>'
$env:GEMINI_API_KEY = ''
$env:RAZORPAY_KEY_ID = ''
$env:RAZORPAY_KEY_SECRET = ''
$env:RAZORPAY_WEBHOOK_SECRET = ''

& mvn spring-boot:run
exit $LASTEXITCODE
