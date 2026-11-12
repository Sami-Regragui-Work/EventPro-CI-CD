package com.eventpro.service;

import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.PaymentStatus;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.request.reservations.CreateReservationRequest;
import com.eventpro.dto.request.reservations.ReservationLineRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.entity.reservations.ReservationLine;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.entity.transactions.Payment;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.entity.users.Client;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.CancellationTooLateException;
import com.eventpro.exception.conflict.IllegalEventStateException;
import com.eventpro.exception.conflict.IllegalReservationStateException;
import com.eventpro.exception.conflict.InsufficientStockException;
import com.eventpro.exception.conflict.PromotionUnavailableException;
import com.eventpro.exception.conflict.QuotaExceededException;
import com.eventpro.exception.internalservererror.SystemException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReservationService {
    private static final Set<ReservationStatus> COMMITTED_STATUSES = EnumSet.of(ReservationStatus.PENDING_PAYMENT, ReservationStatus.CONFIRMED);

    private static final String REFERENCE_PREFIX = "RSV-";
    private static final int REFERENCE_LENGTH = 10;
    private static final int MAX_REFERENCE_ATTEMPTS = 5;
    private static final long RESERVATION_MINUTES = 10;

    private final EventTicketCategoryRepository eventTicketCategoryRepository;
    private final PromotionRepository promotionRepository;
    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final ReservationLineRepository reservationLineRepository;
    private final RefundService refundService;
    private final EventService eventService;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public Reservation create(Long clientId, Long eventId, CreateReservationRequest dto) throws ValidationException {
        Event event = this.eventService.findWithLockPublicById(eventId);
        Instant now = this.clock.instant();

        if (now.isBefore(event.getSalesOpeningDate()))
            throw new IllegalEventStateException("Sales are not open yet for this event");
        if (!event.getSalesClosingDate().isAfter(now))
            throw new IllegalEventStateException("Sales are closed for this event");

        if (dto.getReservationLines() == null || dto.getReservationLines().isEmpty())
            throw new ValidationException("A reservation needs at least one ticket line");

        Client client = this.clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client " + clientId + " not found"));

        List<ReservationLineRequest> reservationLineDtos = dto.getReservationLines().stream().sorted(Comparator.comparing(ReservationLineRequest::getEventTicketCategoryId)).toList();

        long categoriesCount = reservationLineDtos.stream().map(ReservationLineRequest::getEventTicketCategoryId).distinct().count();

        if (categoriesCount != reservationLineDtos.size())
            throw new ValidationException("Each ticket category may appear only once in a reservation");

        List<ReservationLine> reservationLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ReservationLineRequest reservationLineDto : reservationLineDtos) {
            EventTicketCategory eventTicketCategory = this.eventTicketCategoryRepository
                    .findWithLockById(reservationLineDto.getEventTicketCategoryId())
                    .filter(eventTicketCategoryFilter -> eventTicketCategoryFilter.getEvent().getId().equals(eventId))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ticket category " + reservationLineDto.getEventTicketCategoryId() + " not found for event " + eventId));

            int quantity = reservationLineDto.getQuantity();
            this.checkStock(eventTicketCategory, quantity);
            this.checkClientQuota(eventTicketCategory, client.getId(), quantity);

            BigDecimal unitPrice = eventTicketCategory.getUnitPrice();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineSubtotal);

            reservationLines.add(
                    ReservationLine.builder()
                            .quantity(quantity)
                            .fixedUnitPrice(unitPrice)
                            .subtotal(lineSubtotal)
                            .eventTicketCategory(eventTicketCategory)
                            .build()
            );
        }

        Promotion promotion = this.findUsablePromotion(eventId, dto.getPromotionCode());
        BigDecimal discount = promotion == null ? BigDecimal.ZERO : promotion.calculateDiscount(subtotal);
        if (promotion != null) {
            promotion.recordUsage();
        }

        Reservation reservation = Reservation.builder()
                .reference(this.generateUniqueReference())
                .creationDate(now)
                .expirationDate(now.plus(ReservationService.RESERVATION_MINUTES, ChronoUnit.MINUTES))
                .totalAmount(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP))
                .client(client)
                .promotion(promotion)
                .build();

        try {
            Reservation savedReservation = this.reservationRepository.saveAndFlush(reservation);
            reservationLines.forEach(reservationLine -> reservationLine.setReservation(savedReservation));
            savedReservation.setReservationLines(this.reservationLineRepository.saveAll(reservationLines));
            return savedReservation;
        } catch (DataIntegrityViolationException raceLost) {
            throw new SystemException("Could not store the reservation reference", raceLost);
        }
    }

    @Transactional(readOnly = true)
    public Reservation getByReference(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.reservationRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reference + " not found"));
        ReservationService.checkOwnership(reservation, clientId, eventId, reference);
        return reservation;
    }

    public Reservation cancelByClient(Long clientId, Long eventId, String reference) {
        Reservation reservation = this.reservationRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reference + " not found"));
        ReservationService.checkOwnership(reservation, clientId, eventId, reference);
        Instant now = this.clock.instant();

        switch (reservation.getStatus()) {
            case CONFIRMED -> {
                Instant eventStartDate = reservation.getReservationLines().getFirst().getEventTicketCategory().getEvent().getStartDate();
                boolean startsTwoDaysAheadOrLater = Duration.between(now, eventStartDate).compareTo(Duration.ofHours(48)) >= 0;
                if (!startsTwoDaysAheadOrLater)
                    throw new CancellationTooLateException();

                this.refundService.refundConfirmedReservation(reservation, now);
            }
            case PENDING_PAYMENT -> this.refundService.cancelPendingReservation(reservation);
            default -> throw new IllegalReservationStateException();
        }

        // The refund committed in its own transaction (REQUIRES_NEW), so the entity this
        // transaction still holds answered with the pre-cancel status. Usually the endpoint
        // re-reads through a shared persistence context in the same request, which would
        // serve exactly that stale cached copy; dropping the context first makes the
        // re-read hit the committed database state.
        this.entityManager.clear();

        return this.reservationRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reference + " not found"));
    }

    public void expire(Long reservationId) {
        Reservation reservation = this.reservationRepository.findWithLockById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + reservationId + " not found"));

        if (!reservation.isExpiredAt(this.clock.instant()))
            return;

        reservation.transitionTo(ReservationStatus.EXPIRED);

        this.expirePendingPayment(reservation);

        Promotion appliedPromotion = reservation.getPromotion();
        if (appliedPromotion != null) {
            Promotion lockedPromotion = this.promotionRepository.findWithLockById(appliedPromotion.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion " + appliedPromotion.getId() + " not found"));
            lockedPromotion.releaseUsage();
        }
    }

    private Payment expirePendingPayment(Reservation reservation) {
        Payment pendingPayment = reservation.getPayment();

        if (pendingPayment == null || pendingPayment.getStatus() != PaymentStatus.PENDING)
            return null;

        pendingPayment.transitionTo(PaymentStatus.FAILED);
        return pendingPayment;
    }

    private static void checkOwnership(Reservation reservation, Long clientId, Long eventId, String reference) {
        boolean belongsToClientAndEvent = reservation.getClient().getId().equals(clientId)
                && reservation.getReservationLines().stream()
                        .allMatch(reservationLine -> reservationLine.getEventTicketCategory().getEvent().getId().equals(eventId));

        if (!belongsToClientAndEvent)
            throw new ResourceNotFoundException("Reservation " + reference + " not found");
    }

    private void checkStock(EventTicketCategory eventTicketCategory, int requestedQuantity) {
        long reservationLinesQuantity = this.reservationLineRepository.sumQuantityByEventTicketCategoryIdAndReservationStatuses(eventTicketCategory.getId(), ReservationService.COMMITTED_STATUSES);

        long availableQuantity = eventTicketCategory.getTotalQuantity() - reservationLinesQuantity;

        if (requestedQuantity > availableQuantity)
            throw new InsufficientStockException("Only " + availableQuantity + " tickets left in this category");
    }

    private void checkClientQuota(EventTicketCategory eventTicketCategory, Long clientId, int requestedQuantity) {
        long clientReservationLinesQuantity = this.reservationLineRepository.sumQuantityByEventTicketCategoryIdAndClientIdAndReservationStatuses(eventTicketCategory.getId(), clientId, ReservationService.COMMITTED_STATUSES);

        if (clientReservationLinesQuantity + requestedQuantity > eventTicketCategory.getMaxQuotaPerClient())
            throw new QuotaExceededException("Limit is " + eventTicketCategory.getMaxQuotaPerClient() + " tickets per client");
    }

    private Promotion findUsablePromotion(Long eventId, String code) {
        if (code == null || code.isBlank())
            return null;

        String normalizedCode = Promotion.normalizeCode(code);
        Promotion promotion = this.promotionRepository.findWithLockByEventIdAndCode(eventId, normalizedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion code " + normalizedCode + " not found"));
        if (!promotion.isUsableOn(LocalDate.now(this.clock))) {
            throw new PromotionUnavailableException();
        }
        return promotion;
    }

    private String generateUniqueReference() {
        int attempt = ReservationService.MAX_REFERENCE_ATTEMPTS;

        while (attempt > 0) {
            String reference = CodeGenerator.randomCode(ReservationService.REFERENCE_PREFIX, ReservationService.REFERENCE_LENGTH);
            if (!this.reservationRepository.existsByReference(reference)) {
                return reference;
            }

            attempt--;
        }
        throw new SystemException("Could not generate a unique reservation reference");
    }
}
