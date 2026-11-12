package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.repository.TicketCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TicketCategoryServiceIT {
    @Autowired TicketCategoryService ticketCategoryService;
    @Autowired TicketCategoryRepository ticketCategoryRepository;

    @AfterEach
    void clearData() {
        this.ticketCategoryRepository.deleteAllInBatch();
    }

    @Test
    void concurrentFindOrCreateReturnsOneRow() throws Exception {
        String categoryName = "Gala Night " + UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<TicketCategoryService.FindOrCreateTicketCategory>> pendingResults = new ArrayList<>();
        for (int workerNumber = 0; workerNumber < 4; workerNumber++) {
            pendingResults.add(executor.submit((Callable<TicketCategoryService.FindOrCreateTicketCategory>) () ->
                    this.ticketCategoryService.findOrCreate(categoryName)));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));

        List<Long> resultingCategoryIds = new ArrayList<>();
        for (Future<TicketCategoryService.FindOrCreateTicketCategory> pendingResult : pendingResults) {
            resultingCategoryIds.add(pendingResult.get().ticketCategory().getId());
        }

        assertEquals(1, resultingCategoryIds.stream().distinct().count());

        TicketCategory storedCategory = this.ticketCategoryRepository
                .findById(resultingCategoryIds.getFirst()).orElseThrow();
        assertEquals(TicketCategory.normalize(categoryName), storedCategory.getName());
    }
}
