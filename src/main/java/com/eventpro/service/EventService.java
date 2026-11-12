package com.eventpro.service;

import com.eventpro.domain.EventDates;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.dto.request.events.CreateEventRequest;
import com.eventpro.dto.request.events.UpdateEventRequest;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.IllegalEventStateException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.users.Organizer;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.repository.EventRepository;
import com.eventpro.repository.EventTicketCategoryRepository;
import com.eventpro.repository.OrganizerRepository;
import com.eventpro.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class EventService {
    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;
    private final EventTicketCategoryRepository eventTicketCategoryRepository;
    private final ReservationRepository reservationRepository;
    private final RefundService refundService;
    private final EventMapper mapper;
    private final Clock clock;

    public Event create(Long organizerId, CreateEventRequest dto) throws ValidationException {
        Organizer organizer = this.organizerRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer " + organizerId + " not found"));

        EventDates.of(dto.getStartDate(), dto.getEndDate(), dto.getSalesOpeningDate(), dto.getSalesClosingDate());

        Event event = this.mapper.toEntity(dto);
        event.setOrganizer(organizer);
        return this.eventRepository.save(event);
    }

    public Event update(Long organizerId, Long eventId, UpdateEventRequest dto) throws ValidationException {
        Event event = this.lockOwnedEvent(organizerId, eventId);
        event.requireModifiable();

        if (!event.isDraft()) {
            List<String> triedToChangeLocked = EventService.lockedChanges(dto, event);
            if (!triedToChangeLocked.isEmpty()) {
                throw new IllegalEventStateException("Cannot change after publication: " + String.join(", ", triedToChangeLocked));
            }
        }

        // Capacity only stays editable while the event is a draft, so the categories
        // already attached to it must still fit under the requested capacity.
        if (dto.getCapacity() != null) {
            long categoryTotal = this.eventTicketCategoryRepository.sumTotalQuantityByEventId(eventId);
            if (dto.getCapacity() < categoryTotal) {
                throw new ValidationException("Capacity (" + dto.getCapacity()
                        + ") cannot be lower than the category totals (" + categoryTotal + ")");
            }
        }

        EventDates.of(event.getStartDate(), event.getEndDate(), event.getSalesOpeningDate(), event.getSalesClosingDate())
                .override(dto.getStartDate(), dto.getEndDate(), dto.getSalesOpeningDate(), dto.getSalesClosingDate());

        this.mapper.updateEntity(dto, event);
        this.completeIfPast(event, this.clock.instant());
        return event;
    }

    public Event publish(Long organizerId, Long eventId) {
        Event event = this.lockOwnedEvent(organizerId, eventId);
        event.assertCanTransitionTo(EventStatus.PUBLISHED);
        if (!event.getSalesClosingDate().isAfter(this.clock.instant()))
            throw new IllegalEventStateException("Sales have already closed");

        if (event.getEndDate() != null && !event.getEndDate().isAfter(this.clock.instant()))
            throw new IllegalEventStateException("Event has already ended");

        if (this.eventTicketCategoryRepository.countByEventId(eventId) == 0)
            throw new IllegalEventStateException("Publish requires at least one ticket category");

        event.transitionTo(EventStatus.PUBLISHED);
        return event;
    }

    public Event cancel(Long organizerId, Long eventId) {
        Event event = this.lockOwnedEvent(organizerId, eventId);
        event.transitionTo(EventStatus.CANCELED);

        for (Reservation pendingReservation : this.reservationRepository.findByEventIdAndStatus(eventId, ReservationStatus.PENDING_PAYMENT)) {
            this.refundService.cancelPendingReservation(pendingReservation);
        }

        for (Reservation confirmedReservation : this.reservationRepository.findByEventIdAndStatus(eventId, ReservationStatus.CONFIRMED)) {
            this.refundService.refundConfirmedReservation(confirmedReservation, this.clock.instant());
        }

        return event;
    }

    public Event end(Long organizerId, Long eventId) throws ValidationException {
        Event event = this.lockOwnedEvent(organizerId, eventId);
        event.assertCanTransitionTo(EventStatus.COMPLETED);

        if (event.getStartDate().isAfter(this.clock.instant()))
            throw new ValidationException("Event has not started yet");

        Instant now = this.clock.instant();
        event.setEndDate(now);
        // Ending is completing: the event leaves PUBLISHED in the same transaction,
        // so the response already carries the COMPLETED status.
        this.completeIfPast(event, now);
        return event;
    }

    @Transactional(readOnly = true)
    public Page<Event> findPublic(Pageable pageable) {
        return this.eventRepository.findByStatusAndSalesClosingDateAfter(EventStatus.PUBLISHED, this.clock.instant(), pageable);
    }

    @Transactional(readOnly = true)
    public Event findPublicById(Long id) {
        return this.eventRepository.findByIdAndStatus(id, EventStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
    }

    public Event findWithLockPublicById(Long id) {
        Event event = this.eventRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + id + " not found"));
        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new ResourceNotFoundException("Event " + id + " not found");
        return event;
    }

    @Transactional(readOnly = true)
    public Page<Event> findByOrganizer(Long organizerId, Pageable pageable) {
        return this.eventRepository.findByOrganizerId(organizerId, pageable);
    }

    public Event ownedEvent(Long organizerId, Long eventId) {
        return EventService.checkOwner(organizerId, eventId, this.eventRepository.findById(eventId));
    }

    public Event lockOwnedEvent(Long organizerId, Long eventId) {
        return EventService.checkOwner(organizerId, eventId, this.eventRepository.findWithLockById(eventId));
    }

    public void completeIfOver(Long eventId) {
        Event event = this.eventRepository.findWithLockById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
        this.completeIfPast(event, this.clock.instant());
    }

    private void completeIfPast(Event event, Instant now) {
        if (event.getStatus() != EventStatus.PUBLISHED)
            return;
        if (event.getEndDate() == null || event.getEndDate().isAfter(now))
            return;
        event.transitionTo(EventStatus.COMPLETED);
    }

    private static Event checkOwner(Long organizerId, Long eventId, Optional<Event> found) {
        Event event = found.orElseThrow(() -> new ResourceNotFoundException("Event " + eventId + " not found"));
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new ResourceNotFoundException("Event " + eventId + " not found");
        }
        return event;
    }

    private static List<String> lockedChanges(UpdateEventRequest dto, Event event) {
        List<String> changes = new ArrayList<>();
        if (EventService.isChanged(dto.getTitle(), event.getTitle())) changes.add("title");
        if (EventService.isChanged(dto.getStartDate(), event.getStartDate())) changes.add("startDate");
        if (EventService.isChanged(dto.getSalesOpeningDate(), event.getSalesOpeningDate())) changes.add("salesOpeningDate");
        if (EventService.isChanged(dto.getSalesClosingDate(), event.getSalesClosingDate())) changes.add("salesClosingDate");
        if (EventService.isChanged(dto.getCapacity(), event.getCapacity())) changes.add("capacity");
        return changes;
    }

    private static <T> boolean isChanged(T desired, T current) {
        return desired != null && !desired.equals(current);
    }
}
