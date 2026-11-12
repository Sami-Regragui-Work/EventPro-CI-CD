package com.eventpro.service;

import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.request.tickets.AttachTicketCategoryRequest;
import com.eventpro.dto.request.tickets.UpdateTicketCategoryRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.DuplicateTicketCategoryException;
import com.eventpro.exception.conflict.IllegalEventStateException;
import com.eventpro.exception.conflict.InsufficientStockException;
import com.eventpro.exception.conflict.TicketCategoryInUseException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.EventTicketCategoryMapper;
import com.eventpro.repository.ReservationLineRepository;
import com.eventpro.repository.EventTicketCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class EventTicketCategoryService {
    private static final Set<ReservationStatus> COMMITTED_STATUSES =
            EnumSet.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED);

    private final EventService eventService;
    private final TicketCategoryService ticketCategoryService;
    private final EventTicketCategoryRepository eventTicketCategoryRepository;
    private final ReservationLineRepository reservationLineRepository;
    private final EventTicketCategoryMapper mapper;

    public record CategoryStock(EventTicketCategory category, long reservedQuantity, long soldQuantity, long availableQuantity) {}

    @Transactional(readOnly = true)
    public List<CategoryStock> list(Long organizerId, Long eventId) {
        this.eventService.ownedEvent(organizerId, eventId);
        List<EventTicketCategory> categories = this.eventTicketCategoryRepository.findByEventId(eventId);

        Map<Long, ReservationLineRepository.CategoryStockSum> sumsByCategoryId = new HashMap<>();
        for (ReservationLineRepository.CategoryStockSum sum : this.reservationLineRepository
                .sumStockByEventId(eventId, ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED, COMMITTED_STATUSES)) {
            sumsByCategoryId.put(sum.getEventTicketCategoryId(), sum);
        }

        List<CategoryStock> stocks = new ArrayList<>();
        for (EventTicketCategory category : categories) {
            ReservationLineRepository.CategoryStockSum sum = sumsByCategoryId.get(category.getId());
            long reserved = sum == null ? 0L : sum.getReservedQuantity();
            long sold = sum == null ? 0L : sum.getSoldQuantity();
            stocks.add(new CategoryStock(category, reserved, sold, category.getTotalQuantity() - reserved - sold));
        }
        return stocks;
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> availableSeatsByEventId(Collection<Long> eventIds) {
        Map<Long, Long> availableByEventId = new HashMap<>();
        if (eventIds.isEmpty())
            return availableByEventId;

        for (EventTicketCategoryRepository.EventTotalSum total : this.eventTicketCategoryRepository.sumTotalByEventIds(eventIds))
            availableByEventId.put(total.getEventId(), total.getTotalQuantity());

        for (ReservationLineRepository.EventStockSum committed : this.reservationLineRepository
                .sumCommittedByEventIds(eventIds, COMMITTED_STATUSES)) {
            availableByEventId.computeIfPresent(committed.getEventId(),
                    (eventId, remaining) -> remaining - committed.getCommittedQuantity());
        }
        return availableByEventId;
    }

    public EventTicketCategoryService.CategoryStock attach(Long organizerId, Long eventId, AttachTicketCategoryRequest dto) throws ValidationException {
        Event event = this.eventService.lockOwnedEvent(organizerId, eventId);
        EventTicketCategoryService.requireDraft(event, "Categories can only be attached while the event is DRAFT");

        TicketCategory ticketCategory = this.ticketCategoryService.findOrCreate(dto.getName()).ticketCategory();

        if (this.eventTicketCategoryRepository.existsByEventIdAndTicketCategoryId(eventId, ticketCategory.getId()))
            throw new DuplicateTicketCategoryException();

        EventTicketCategoryService.checkQuota(dto.getMaxQuotaPerClient(), dto.getTotalQuantity());
        this.checkCapacity(event, 0, dto.getTotalQuantity());

        EventTicketCategory eventCategory = this.mapper.toEntity(dto);
        eventCategory.setEvent(event);
        eventCategory.setTicketCategory(ticketCategory);

        try {
            eventCategory = this.eventTicketCategoryRepository.saveAndFlush(eventCategory);
        } catch (DataIntegrityViolationException raceLost) {
            throw new DuplicateTicketCategoryException();
        }

        // A freshly attached category has no reservations yet: nothing is reserved or
        // sold, so the available quantity is the whole stock. Attach has to answer with
        // the derived stock shape instead of raw entity fields (an unmapped
        // availableQuantity previously serialized as 0, reading as "sold out").
        return new EventTicketCategoryService.CategoryStock(eventCategory, 0L, 0L, eventCategory.getTotalQuantity());
    }

    public CategoryStock update(Long organizerId, Long eventId, Long eventCategoryId, UpdateTicketCategoryRequest dto) throws ValidationException {
        Event event = this.eventService.lockOwnedEvent(organizerId, eventId);
        event.requireModifiable();

        EventTicketCategory eventCategory = this.findEventCategory(eventId, eventCategoryId);

        if (!event.isDraft()) {
            List<String> locked = new ArrayList<>();
            if (dto.getUnitPrice() != null && dto.getUnitPrice().compareTo(eventCategory.getUnitPrice()) != 0) {
                locked.add("unitPrice");
            }
            if (dto.getMaxQuotaPerClient() != null && !dto.getMaxQuotaPerClient().equals(eventCategory.getMaxQuotaPerClient())) {
                locked.add("maxQuotaPerClient");
            }
            if (!locked.isEmpty()) {
                throw new IllegalEventStateException("Cannot change after publication: " + String.join(", ", locked));
            }
            if (dto.getTotalQuantity() != null && dto.getTotalQuantity() < eventCategory.getTotalQuantity()) {
                throw new IllegalEventStateException("Total quantity can only increase after publication");
            }
        }

        int newAddedQuantity = Objects.requireNonNullElse(dto.getTotalQuantity(), eventCategory.getTotalQuantity());
        int newQuota = Objects.requireNonNullElse(dto.getMaxQuotaPerClient(), eventCategory.getMaxQuotaPerClient());
        EventTicketCategoryService.checkQuota(newQuota, newAddedQuantity);
        this.checkCapacity(event, eventCategory.getTotalQuantity(), newAddedQuantity);

        long committedQuantity = this.reservationLineRepository.sumQuantityByEventTicketCategoryIdAndReservationStatuses(eventCategoryId, EventTicketCategoryService.COMMITTED_STATUSES);

        if (newAddedQuantity < committedQuantity)
            throw new InsufficientStockException("Total quantity cannot be below the " + committedQuantity + " already reserved or sold");

        this.mapper.updateEntity(dto, eventCategory);

        return this.list(organizerId, eventId).stream()
                .filter(stock -> stock.category().getId().equals(eventCategoryId))
                .findFirst()
                .orElseThrow();
    }

    public void remove(Long organizerId, Long eventId, Long eventCategoryId) {
        Event event = this.eventService.lockOwnedEvent(organizerId, eventId);
        EventTicketCategoryService.requireDraft(event, "Categories can only be removed while the event is DRAFT");
        EventTicketCategory eventCategory = this.findEventCategory(eventId, eventCategoryId);

        if (this.reservationLineRepository.existsByEventTicketCategoryId(eventCategoryId))
            throw new TicketCategoryInUseException();

        this.eventTicketCategoryRepository.delete(eventCategory);
    }

    private static void requireDraft(Event event, String message) {
        if (!event.isDraft())
            throw new IllegalEventStateException(message);
    }

    private static void checkQuota(int quota, int total) throws ValidationException {
        if (quota > total)
            throw new ValidationException("Quota per client cannot exceed total quantity");

    }

    private void checkCapacity(Event event, int currentUsedQuantity, int newAddedQuantity) throws ValidationException {
        long resultedTotal = this.eventTicketCategoryRepository.sumTotalQuantityByEventId(event.getId()) - currentUsedQuantity + newAddedQuantity;

        if (resultedTotal > event.getCapacity())
            throw new ValidationException("Total quantity across categories (" + resultedTotal + ") exceeds event capacity (" + event.getCapacity() + ")");
    }

    private EventTicketCategory findEventCategory(Long eventId, Long eventCategoryId) {
        return this.eventTicketCategoryRepository.findByIdAndEventId(eventCategoryId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket category " + eventCategoryId + " not found for event " + eventId));
    }
}
