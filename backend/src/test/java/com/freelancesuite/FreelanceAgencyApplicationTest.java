package com.freelancesuite;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FreelanceAgencyApplicationTest {
    private static final String DATABASE_URL = "jdbc:postgresql://neon.example/agency?sslmode=require";

    @Test
    void devProfileAllowsOnlyH2BeforeHibernateStarts() {
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateDevelopmentDatabase(
                new String[]{"dev"}, "jdbc:h2:mem:freelance;MODE=PostgreSQL"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateDevelopmentDatabase(
                new String[]{"dev"}, "jdbc:postgresql://neon.example/database"));
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateDevelopmentDatabase(
                new String[]{"prod"}, "jdbc:postgresql://neon.example/database"));
    }

    @Test
    void prodProfileRejectsMissingOrInsecureLaunchSettings() {
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                DATABASE_URL, "valid-jwt-secret-that-is-at-least-32-characters", "", "", "resend-key", "sender@example.test"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                DATABASE_URL, "valid-jwt-secret-that-is-at-least-32-characters", "http://app.example.test", "http://app.example.test",
                "resend-key", "sender@example.test"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                DATABASE_URL, "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://admin.example.test", "resend-key", "sender@example.test"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                DATABASE_URL, "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test/", "resend-key", "sender@example.test"));
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateProductionConfiguration(
                DATABASE_URL, "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test,https://admin.example.test", "resend-key", "Agency <sender@example.test>"));
    }

    @Test
    void prodProfileRequiresEncryptedPostgresAndRejectsWeakOrOtherDatabaseUrls() {
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:postgresql://db.example.test/agency?sslmode=disable",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:postgresql://db.example.test/agency",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test"));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:h2:mem:agency",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test"));
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:postgresql://db.example.test/agency?sslmode=verify-full",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test"));
    }

    @Test
    void migrationTestProfileOnlyAllowsUnencryptedLoopbackPostgres() {
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:postgresql://localhost:5432/agency?sslmode=disable",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test", true));
        assertThrows(IllegalStateException.class, () -> FreelanceAgencyApplication.validateProductionConfiguration(
                "jdbc:postgresql://db.example.test/agency?sslmode=disable",
                "valid-jwt-secret-that-is-at-least-32-characters", "https://app.example.test",
                "https://app.example.test", "resend-key", "sender@example.test", true));
    }

    @Test
    void launchValidationOnlyAppliesLoopbackTlsExceptionWithMigrationTestProfile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/agency?sslmode=disable")
                .withProperty("app.jwt.secret", "valid-jwt-secret-that-is-at-least-32-characters")
                .withProperty("app.email.frontend-base-url", "https://app.example.test")
                .withProperty("app.cors.allowed-origins", "https://app.example.test")
                .withProperty("app.email.resend-api-key", "resend-key")
                .withProperty("app.email.resend-from", "sender@example.test");
        environment.setActiveProfiles("prod", "migration-test");
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validateLaunchConfiguration(environment));

        environment.setActiveProfiles("prod");
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validateLaunchConfiguration(environment));
    }

    @Test
    void launchRequiresExactlyOneExplicitProfile() {
        MockEnvironment environment = new MockEnvironment();
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validateLaunchConfiguration(environment));
        environment.setActiveProfiles("dev", "prod");
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validateLaunchConfiguration(environment));
    }

    @Test
    void paymentConfigurationFailsClosedWhenOnlyPartOfRazorpayIsConfigured() {
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validatePaymentConfiguration("", "", ""));
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validatePaymentConfiguration("rzp_live_id", "", ""));
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validatePaymentConfiguration("rzp_live_id", "key-secret", ""));
        assertThrows(IllegalStateException.class,
                () -> FreelanceAgencyApplication.validatePaymentConfiguration(
                        "rzp_live_id", "key-secret", "short-webhook-secret"));
        assertDoesNotThrow(() -> FreelanceAgencyApplication.validatePaymentConfiguration(
                "rzp_live_id", "key-secret", "webhook-secret-value-at-least-32-chars"));
    }
}
