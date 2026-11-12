package com.eventpro.entity.transactions;

import com.eventpro.entity.Event;
import com.eventpro.domain.enums.DiscountType;
import com.eventpro.entity.reservations.Reservation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Entity @Table(
        name = "promotions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_code",
                columnNames = {"event_id", "code"}
        )
)
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private int usageLimit;

    private int usageCount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Event event;

    @OneToMany(mappedBy = "promotion")
    private List<Reservation> reservations;

    public boolean isUsableOn(LocalDate today) {
        return !today.isBefore(this.startDate) && !today.isAfter(this.endDate) && this.usageCount < this.usageLimit;
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal rawDiscount = switch (this.discountType) {
            case DiscountType.PERCENTAGE -> subtotal.multiply(this.discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case DiscountType.FIXED_AMOUNT -> this.discountValue;
        };
        return rawDiscount.min(subtotal);
    }

    public static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public void recordUsage() {
        this.usageCount++;
    }

    public void releaseUsage() {
        this.usageCount--;
    }
}