package com.freelancesuite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;

@SpringBootApplication
@EnableScheduling
public class FreelanceAgencyApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(FreelanceAgencyApplication.class);
        application.addInitializers(context -> validateLaunchConfiguration(context.getEnvironment()));
        application.run(args);
    }

    static void validateLaunchConfiguration(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        boolean dev = Arrays.asList(profiles).contains("dev");
        boolean prod = Arrays.asList(profiles).contains("prod");
        if (dev == prod) {
            throw new IllegalStateException("Activate exactly one launch profile: dev or prod.");
        }
        if (dev) {
            validateDevelopmentDatabase(profiles, environment.getProperty("spring.datasource.url", ""));
        }
        if (prod) {
            // The migration integration test targets an isolated loopback PostgreSQL service.
            validateProductionConfiguration(
                    environment.getProperty("spring.datasource.url", ""),
                    environment.getProperty("app.jwt.secret", ""),
                    environment.getProperty("app.email.frontend-base-url", ""),
                    environment.getProperty("app.cors.allowed-origins", ""),
                    environment.getProperty("app.email.resend-api-key", ""),
                    environment.getProperty("app.email.resend-from", ""),
                    Arrays.asList(profiles).contains("migration-test"));
            validatePaymentConfiguration(
                    environment.getProperty("app.razorpay.key-id", ""),
                    environment.getProperty("app.razorpay.key-secret", ""),
                    environment.getProperty("app.razorpay.webhook-secret", ""));
        }
    }

    static void validateDevelopmentDatabase(String[] activeProfiles, String datasourceUrl) {
        boolean devProfile = Arrays.asList(activeProfiles).contains("dev");
        if (devProfile && !datasourceUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")) {
            throw new IllegalStateException(
                    "The dev profile only supports disposable H2 databases. Use prod for PostgreSQL.");
        }
    }

    static void validateProductionConfiguration(String datasourceUrl, String jwtSecret, String frontendBaseUrl,
                                                String corsAllowedOrigins, String resendApiKey,
                                                String resendFrom) {
        validateProductionConfiguration(datasourceUrl, jwtSecret, frontendBaseUrl, corsAllowedOrigins,
                resendApiKey, resendFrom, false);
    }

    static void validateProductionConfiguration(String datasourceUrl, String jwtSecret, String frontendBaseUrl,
                                                String corsAllowedOrigins, String resendApiKey,
                                                String resendFrom, boolean migrationTestProfile) {
        requireTlsPostgresUrl(datasourceUrl, migrationTestProfile);
        requireConfigured(jwtSecret, "JWT_SECRET");
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
        }
        requireHttpsOrigin(frontendBaseUrl, "APP_FRONTEND_BASE_URL");
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain the deployed frontend origin.");
        }
        for (String origin : corsAllowedOrigins.split(",")) {
            requireHttpsOrigin(origin.trim(), "CORS_ALLOWED_ORIGINS");
        }
        boolean frontendOriginAllowed = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .anyMatch(frontendBaseUrl::equals);
        if (!frontendOriginAllowed) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must include APP_FRONTEND_BASE_URL.");
        }
        requireConfigured(resendApiKey, "RESEND_API_KEY");
        requireConfigured(resendFrom, "RESEND_FROM");
    }

    private static void requireTlsPostgresUrl(String datasourceUrl, boolean migrationTestProfile) {
        requireConfigured(datasourceUrl, "SPRING_DATASOURCE_URL");
        if (!datasourceUrl.trim().regionMatches(true, 0, "jdbc:postgresql://", 0, "jdbc:postgresql://".length())) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must use a PostgreSQL JDBC URL in the prod profile.");
        }

        URI uri;
        try {
            uri = URI.create(datasourceUrl.trim().substring("jdbc:".length()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must be a valid PostgreSQL JDBC URL.", ex);
        }
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() < 2) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must include a PostgreSQL host and database name.");
        }

        String sslMode = uri.getRawQuery() == null ? "" : Arrays.stream(uri.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2 && "sslmode".equalsIgnoreCase(parts[0]))
                .map(parts -> parts[1])
                .findFirst()
                .orElse("");
        if (Arrays.asList("require", "verify-ca", "verify-full").contains(sslMode.toLowerCase(Locale.ROOT))) {
            return;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        boolean loopbackMigrationDatabase = migrationTestProfile
                && Arrays.asList("localhost", "127.0.0.1", "::1").contains(host)
                && "disable".equalsIgnoreCase(sslMode);
        if (!loopbackMigrationDatabase) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL must set sslmode=require, verify-ca, or verify-full in the prod profile.");
        }
    }

    private static void requireConfigured(String value, String settingName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(settingName + " must be configured before starting the prod profile.");
        }
    }

    static void validatePaymentConfiguration(String razorpayKeyId, String razorpayKeySecret,
                                              String razorpayWebhookSecret) {
        boolean keyIdConfigured = razorpayKeyId != null && !razorpayKeyId.isBlank();
        boolean keySecretConfigured = razorpayKeySecret != null && !razorpayKeySecret.isBlank();
        if (!keyIdConfigured && !keySecretConfigured) return;
        if (!keyIdConfigured || !keySecretConfigured) {
            throw new IllegalStateException("Configure both RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET, or leave both blank.");
        }
        requireConfigured(razorpayWebhookSecret, "RAZORPAY_WEBHOOK_SECRET");
        if (razorpayWebhookSecret.length() < 32) {
            throw new IllegalStateException("RAZORPAY_WEBHOOK_SECRET must be at least 32 characters long.");
        }
    }

    private static void requireHttpsOrigin(String value, String settingName) {
        requireConfigured(value, settingName);
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(settingName + " must be a valid HTTPS origin.", ex);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalStateException(settingName + " must be an HTTPS origin without a path, query, or fragment.");
        }
    }
}
