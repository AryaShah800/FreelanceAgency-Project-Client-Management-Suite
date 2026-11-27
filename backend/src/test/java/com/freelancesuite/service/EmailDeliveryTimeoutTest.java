package com.freelancesuite.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A slow email provider must fail the request quickly (503), not hold the transaction and row lock open. */
class EmailDeliveryTimeoutTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void slowProviderTimesOutWith503() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/emails", exchange -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        EmailDeliveryService service = new EmailDeliveryService();
        ReflectionTestUtils.setField(service, "deliveryMode", "resend");
        ReflectionTestUtils.setField(service, "resendApiKey", "test-key");
        ReflectionTestUtils.setField(service, "resendFrom", "noreply@example.test");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "resendUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/emails");
        ReflectionTestUtils.setField(service, "connectTimeout", Duration.ofSeconds(1));
        ReflectionTestUtils.setField(service, "readTimeout", Duration.ofMillis(300));

        long started = System.nanoTime();
        assertThatThrownBy(() -> service.sendAccountLink("a@example.test", "Subject", "Message", "http://x/y"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3));
    }
}
