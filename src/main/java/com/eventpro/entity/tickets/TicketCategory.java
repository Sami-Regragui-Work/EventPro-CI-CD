package com.eventpro.entity.tickets;

import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Entity @Table(name = "ticket_categories")
@Builder @AllArgsConstructor
@Getter @Setter @NoArgsConstructor
public class TicketCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @PrePersist
    @PreUpdate
    private void normalizeName() {
        this.name = TicketCategory.normalize(this.name);
    }

    public static String normalize(String unnormalizedName) {
        return unnormalizedName.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
    }
}
