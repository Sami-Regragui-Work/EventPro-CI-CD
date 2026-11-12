package com.eventpro.service;

import com.eventpro.TestcontainersConfiguration;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.dto.request.events.UpdateEventRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.repository.EventRepository;
import com.eventpro.repository.EventTicketCategoryRepository;
import com.eventpro.repository.OrganizerRepository;
import com.eventpro.repository.TicketCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EventServiceIT {
    @Autowired EventService eventService;
    @Autowired EventRepository eventRepository;
    @Autowired EventTicketCategoryRepository eventTicketCategoryRepository;
    @Autowired OrganizerRepository organizerRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;
    @Autowired Clock clock;

    @AfterEach
    void clearData() {
        this.eventTicketCategoryRepository.deleteAllInBatch();
        this.eventRepository.deleteAllInBatch();
        this.ticketCategoryRepository.deleteAllInBatch();
        this.organizerRepository.deleteAllInBatch();
    }

    @Test
    void updateRejectsACapacityLowerThanTheCategoryTotals() throws ValidationException {
        EventServiceIT.Fixture fixture = this.fixture("capacityTooLow", 50);

        UpdateEventRequest request = new UpdateEventRequest();
        request.setCapacity(40);

        assertThrows(ValidationException.class,
                () -> this.eventService.update(fixture.organizer().getId(), fixture.event().getId(), request));

        assertEquals(100, this.eventRepository.findById(fixture.event().getId()).orElseThrow().getCapacity());
    }

    @Test
    void updateAcceptsACapacityAboveTheCategoryTotals() throws ValidationException {
        EventServiceIT.Fixture fixture = this.fixture("capacityOk", 50);

        UpdateEventRequest request = new UpdateEventRequest();
        request.setCapacity(60);

        Event updated = this.eventService.update(fixture.organizer().getId(), fixture.event().getId(), request);

        assertEquals(60, updated.getCapacity());
        assertEquals(60, this.eventRepository.findById(fixture.event().getId()).orElseThrow().getCapacity());
    }

    @Test
    void endCompletesAPublishedEventAndSetsItsEndDate() throws ValidationException {
        EventServiceIT.Fixture fixture = this.fixture("ending", 50);

        Event publishedEvent = fixture.event();
        publishedEvent.setStartDate(this.clock.instant().minusSeconds(3_600));
        publishedEvent.transitionTo(EventStatus.PUBLISHED);
        this.eventRepository.save(publishedEvent);

        Instant beforeEnd = this.clock.instant().truncatedTo(ChronoUnit.MICROS);

        Event ended = this.eventService.end(fixture.organizer().getId(), fixture.event().getId());

        Instant afterEnd = this.clock.instant().truncatedTo(ChronoUnit.MICROS);
        assertEquals(EventStatus.COMPLETED, ended.getStatus());
        assertNotNull(ended.getEndDate());
        assertTrue(!ended.getEndDate().isBefore(beforeEnd) && !ended.getEndDate().isAfter(afterEnd));
    }

    private EventServiceIT.Fixture fixture(String label, int categoryTotal) {
        String suffix = label + "-" + UUID.randomUUID();

        Organizer organizer = this.organizerRepository.save(Organizer.builder()
                .name("Organizer " + label)
                .email("organizer-" + suffix + "@it.eventpro.dev")
                .organisationName("Organisation " + label)
                .build());

        Instant now = this.clock.instant();
        Event event = this.eventRepository.save(Event.builder()
                .title("Event " + label)
                .location("Hall " + label)
                .startDate(now.plus(30, ChronoUnit.DAYS))
                .salesOpeningDate(now.minus(1, ChronoUnit.HOURS))
                .salesClosingDate(now.plus(1, ChronoUnit.HOURS))
                .capacity(100)
                .organizer(organizer)
                .build());

        TicketCategory ticketCategory = this.ticketCategoryRepository
                .save(TicketCategory.builder().name("Category_" + suffix.toUpperCase()).build());

        this.eventTicketCategoryRepository.save(EventTicketCategory.builder()
                .unitPrice(new BigDecimal("50.00"))
                .totalQuantity(categoryTotal)
                .maxQuotaPerClient(categoryTotal)
                .event(event)
                .ticketCategory(ticketCategory)
                .build());

        return new EventServiceIT.Fixture(suffix, organizer, event);
    }

    private record Fixture(String suffix, Organizer organizer, Event event) {
    }
}
