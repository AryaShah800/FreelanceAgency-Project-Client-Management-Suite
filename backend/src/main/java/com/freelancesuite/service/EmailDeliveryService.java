package com.freelancesuite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);
    // Build the HTTP client only when a real email is sent. Creating the JDK
    // client eagerly can prevent the app from starting in restricted test and
    // container environments, even when the console delivery mode is used.
    private RestClient restClient;
    @Value("${app.email.delivery-mode:resend}") private String deliveryMode;
    @Value("${app.email.resend-api-key:}") private String resendApiKey;
    @Value("${app.email.resend-from:}") private String resendFrom;
    @Value("${app.email.frontend-base-url:http://localhost:5173}") private String frontendBaseUrl;

    public String link(String path, String token) {
        String base = frontendBaseUrl.replaceAll("/+$", "");
        return base + path + "?token=" + java.net.URLEncoder.encode(token, java.nio.charset.StandardCharsets.UTF_8);
    }

    public void sendAccountLink(String to, String subject, String message, String url) {
        String escapedUrl = escape(url);
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#222\">"
                + "<h2>Freelance Agency Suite</h2><p>" + escape(message) + "</p>"
                + "<p><a href=\"" + escapedUrl + "\">Continue securely</a></p>"
                + "<p>If you did not request this, you can ignore this email.</p></div>";
        send(to, subject, html);
    }

    private void send(String to, String subject, String html) {
        if ("console".equalsIgnoreCase(deliveryMode)) {
            log.warn("Development email: to={} subject={} html={}", to, subject, html);
            return;
        }
        if (!"resend".equalsIgnoreCase(deliveryMode) || resendApiKey.isBlank() || resendFrom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is not configured. Configure Resend before using account email flows.");
        }
        try {
            getRestClient().post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", resendFrom, "to", List.of(to), "subject", subject, "html", html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.error("Resend rejected an account email with HTTP status {}", ex.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email could not be delivered right now.");
        } catch (RuntimeException ex) {
            log.error("Resend account email delivery failed", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email could not be delivered right now.");
        }
    }

    private RestClient getRestClient() {
        if (restClient == null) {
            restClient = RestClient.create();
        }
        return restClient;
    }

    private String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
