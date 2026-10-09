package com.freelancesuite.service;

import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Proposal;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.ProposalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ProposalConversionServiceConcurrencyTest {

    @Autowired
    private ProposalConversionService proposalConversionService;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private AgencyRepository agencyRepository;

    @Test
    void testConcurrentDoubleSign() throws InterruptedException {
        Agency agency = Agency.builder().name("Test").build();
        agencyRepository.save(agency);

        Proposal p = Proposal.builder()
                .agency(agency)
                .shareToken("concurrent-test-token")
                .clientName("Client")
                .estimatedBudget(BigDecimal.TEN)
                .isSigned(false)
                .build();
        proposalRepository.save(p);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    proposalConversionService.recordProposalAcceptance("concurrent-test-token", "Signer");
                    successCount.incrementAndGet();
                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown();
        done.await();

        assertEquals(1, successCount.get(), "Exactly one request should succeed");
        assertEquals(1, conflictCount.get(), "Exactly one request should fail with 409 Conflict");
    }
}
