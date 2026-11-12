package com.eventpro.entity.transactions;

import com.eventpro.domain.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "refunds")
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;
    private String reason;

    @Column(nullable = false)
    private Instant requestDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Payment payment;
}