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

        for (int i = 0; i < 10; i++) service.enforceRateLimit(101L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.enforceRateLimit(101L));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        service.enforceRateLimit(102L);
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
