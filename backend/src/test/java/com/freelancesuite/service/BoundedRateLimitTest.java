package com.freelancesuite.service;

import tools.jackson.databind.ObjectMapper;
import com.freelancesuite.repository.InvoiceRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TimeEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class BoundedRateLimitTest {

    @Test
    void aiGenerationLimitAllowsTenRequestsThenReturnsTooManyRequests() {
        AIService service = new AIService(mock(WebClient.Builder.class), new ObjectMapper());

        long t = 1_000_000L;
        // Requests 11 s apart clear the cooldown; the 10-per-10-minutes window still applies.
        for (int i = 0; i < 10; i++) service.enforceRateLimit(101L, t + i * 11_000L);

        long eleventh = t + 10 * 11_000L;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.enforceRateLimit(101L, eleventh));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        service.enforceRateLimit(102L, eleventh);
    }

    @Test
    void aiGenerationHasACooldownBetweenDrafts() {
        AIService service = new AIService(mock(WebClient.Builder.class), new ObjectMapper());
        service.enforceRateLimit(103L, 1_000L);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, assertThrows(ResponseStatusException.class,
                () -> service.enforceRateLimit(103L, 5_000L)).getStatusCode());
        service.enforceRateLimit(103L, 12_000L);
    }

    @Test
    void aiGenerationStopsAtTheDailyCap() {
        AIService service = new AIService(mock(WebClient.Builder.class), new ObjectMapper());
        long t = 0L;
        // 50 drafts spread across 10-minute windows (5 per window, 2 minutes apart).
        for (int i = 0; i < 50; i++) service.enforceRateLimit(104L, t + i * 120_000L);
        long next = t + 50 * 120_000L;
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.enforceRateLimit(104L, next));
        assertEquals("Daily AI generation limit reached.", exception.getReason());
        service.enforceRateLimit(104L, t + 24 * 60 * 60 * 1000L + 1);
    }

    @Test
    void pdfGenerationLimitAllowsTenRequestsThenReturnsTooManyRequests() {
        InvoiceService service = invoiceService();

        for (int i = 0; i < 10; i++) service.enforcePdfRateLimit(201L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.enforcePdfRateLimit(201L));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        service.enforcePdfRateLimit(202L);
    }

    @Test
    void concurrentPdfRequestsCannotExceedThePerUserLimit() throws Exception {
        InvoiceService service = invoiceService();
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> requests = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                requests.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out.");
                    try {
                        service.enforcePdfRateLimit(301L);
                        accepted.incrementAndGet();
                    } catch (ResponseStatusException exception) {
                        if (exception.getStatusCode() != HttpStatus.TOO_MANY_REQUESTS) throw exception;
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> request : requests) request.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(10, accepted.get());
    }

    private InvoiceService invoiceService() {
        return new InvoiceService(mock(InvoiceRepository.class), mock(ProjectRepository.class),
                mock(TimeEntryRepository.class));
    }
}
