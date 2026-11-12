package com.eventpro.entity.transactions;

import com.eventpro.domain.enums.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromotionTest {
    private static final LocalDate TODAY = LocalDate.parse("2026-06-15");

    @Test
    void normalizesCode() {
        assertEquals("GALA NIGHT", Promotion.normalizeCode("  gala Night "));
        assertEquals("GALA_NIGHT", Promotion.normalizeCode("Gala_Night"));
    }

    @Test
    void discountIsCappedAtSubtotal() {
        Promotion largerThanSubtotal = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "200.00",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), 10, 0);

        assertEquals(new BigDecimal("100"), largerThanSubtotal.calculateDiscount(new BigDecimal("100")));
        assertEquals(new BigDecimal("80.00"), PromotionTest.promotion(DiscountType.PERCENTAGE, "80",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), 10, 0)
                .calculateDiscount(new BigDecimal("100")));
    }

    @Test
    void percentageRoundsHalfUp() {
        Promotion halfPercentage = PromotionTest.promotion(DiscountType.PERCENTAGE, "50",
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-30"), 10, 0);

        assertEquals(new BigDecimal("5.03"), halfPercentage.calculateDiscount(new BigDecimal("10.05")));
    }

    @Test
    void usableOnlyWithinDatesAndLimit() {
        Promotion usablePromotion = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "10.00",
                TODAY.minusDays(1), TODAY.plusDays(1), 5, 2);
        assertTrue(usablePromotion.isUsableOn(TODAY));

        Promotion notYetOpen = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "10",
                TODAY.plusDays(1), TODAY.plusDays(5), 5, 0);
        assertFalse(notYetOpen.isUsableOn(TODAY));

        Promotion alreadyClosed = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "10",
                TODAY.minusDays(5), TODAY.minusDays(1), 5, 0);
        assertFalse(alreadyClosed.isUsableOn(TODAY));

        Promotion usedUp = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "10",
                TODAY.minusDays(1), TODAY.plusDays(1), 5, 5);
        assertFalse(usedUp.isUsableOn(TODAY));
    }

    @Test
    void usageCountsMoveInBothDirections() {
        Promotion promotion = PromotionTest.promotion(DiscountType.FIXED_AMOUNT, "10",
                TODAY, TODAY, 5, 0);

        promotion.recordUsage();
        promotion.recordUsage();
        assertEquals(2, promotion.getUsageCount());

        promotion.releaseUsage();
        assertEquals(1, promotion.getUsageCount());
    }

    private static Promotion promotion(DiscountType discountType, String discountValue,
                                       LocalDate startDate, LocalDate endDate, int usageLimit, int usageCount) {
        return Promotion.builder()
                .code("GALA")
                .discountType(discountType)
                .discountValue(new BigDecimal(discountValue))
                .startDate(startDate)
                .endDate(endDate)
                .usageLimit(usageLimit)
                .usageCount(usageCount)
                .build();
    }
}
