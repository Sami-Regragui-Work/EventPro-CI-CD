package com.eventpro.service;

import com.eventpro.domain.CodeGenerator;
import com.eventpro.domain.enums.DiscountType;
import com.eventpro.dto.request.CreatePromotionRequest;
import com.eventpro.entity.Event;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.exception.conflict.PromotionInUseException;
import com.eventpro.exception.internalservererror.SystemException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.PromotionMapper;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class PromotionService {
    private static final int CODE_LENGTH = 8;
    private static final int MAX_ATTEMPTS = 5;

    private final EventService eventService;
    private final PromotionRepository repository;
    private final ReservationRepository reservationRepository;
    private final PromotionMapper mapper;

    @Transactional(readOnly = true)
    public List<Promotion> list(Long organizerId, Long eventId) {
        this.eventService.ownedEvent(organizerId, eventId);
        return this.repository.findByEventId(eventId);
    }

    public Promotion create(Long organizerId, Long eventId, CreatePromotionRequest dto) throws ValidationException {
        Event event = this.eventService.ownedEvent(organizerId, eventId);
        event.requireModifiable();
        PromotionService.checkDiscount(dto);

        Promotion promotion = this.mapper.toEntity(dto);
        promotion.setCode(this.generateUniqueCode(eventId));
        promotion.setEvent(event);
        promotion.setUsageCount(0);

        try {
            return this.repository.saveAndFlush(promotion);
        } catch (DataIntegrityViolationException raceLost) {
            throw new SystemException("Could not store the promotion code", raceLost);
        }
    }

    public void delete(Long organizerId, Long eventId, Long promotionId) {
        Event event = this.eventService.ownedEvent(organizerId, eventId);
        event.requireModifiable();

        Promotion promotion = this.repository.findByIdAndEventId(promotionId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Promotion " + promotionId + " not found for event " + eventId));

        if (promotion.getUsageCount() > 0) {
            throw new PromotionInUseException();
        }

        // Usage counters are released when reservations close, so a spent promotion can
        // still be referenced from reservations (audit history). Refuse the delete then:
        // the database FK on reservations.promotion_id would otherwise make the delete
        // fail as an unmapped 500.
        if (this.reservationRepository.existsByPromotionId(promotion.getId())) {
            throw new PromotionInUseException("Promotion " + promotionId + " is attached to reservations and cannot be deleted");
        }

        this.repository.delete(promotion);
    }

    private static void checkDiscount(CreatePromotionRequest dto) throws ValidationException {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ValidationException("End date must be on or after start date");
        }
        if (dto.getDiscountType() == DiscountType.PERCENTAGE
                && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("Percentage discount cannot exceed 100");
        }
    }

    private String generateUniqueCode(Long eventId) {
        for (int attempt = 0; attempt < PromotionService.MAX_ATTEMPTS; attempt++) {
            String code = CodeGenerator.randomCode("PROMO-", PromotionService.CODE_LENGTH);
            if (!this.repository.existsByEventIdAndCode(eventId, code)) {
                return code;
            }
        }
        throw new SystemException("Could not generate a unique promotion code");
    }
}
