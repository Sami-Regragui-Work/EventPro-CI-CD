package com.eventpro.service;

import com.eventpro.entity.reservations.Reservation;
import com.eventpro.repository.ReservationLineRepository;
import com.eventpro.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final ReservationLineRepository reservationLineRepository;
    private final ReservationRepository reservationRepository;

    public List<EventSalesSummary> organizerSales(Long organizerId) {
        return this.reservationLineRepository.sumSalesByOrganizerId(organizerId).stream()
                .map(salesSum -> new DashboardService.EventSalesSummary(
                        salesSum.getEventId(),
                        salesSum.getTitle(),
                        salesSum.getTicketsSold(),
                        salesSum.getRevenue()))
                .toList();
    }

    public Page<Reservation> reservationHistory(Pageable pageable) {
        return this.reservationRepository.findAll(pageable);
    }

    public record EventSalesSummary(Long eventId, String title, long ticketsSold, BigDecimal revenue) {}
}
