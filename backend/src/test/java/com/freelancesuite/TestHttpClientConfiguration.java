package com.freelancesuite;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Disables outbound HTTP in tests so integrations cannot reach real services. */
@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestHttpClientConfiguration {
    @Bean
    WebClient.Builder testWebClientBuilder() {
        return WebClient.builder().exchangeFunction(request ->
                Mono.error(new IllegalStateException("Outbound HTTP is disabled in tests.")));
    }
}
