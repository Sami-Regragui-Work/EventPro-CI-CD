package com.eventpro.controller.dashboards;

import com.eventpro.dto.response.dashboards.EventSalesResponse;
import com.eventpro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/organizers/{organizerId}/dashboard")
@RequiredArgsConstructor
public class OrganizerDashboardController {
    private final DashboardService service;

    @GetMapping("/sales")
    public List<EventSalesResponse> sales(@PathVariable Long organizerId) {
        return this.service.organizerSales(organizerId).stream()
                .map(salesSummary -> EventSalesResponse.builder()
                        .eventId(salesSummary.eventId())
                        .title(salesSummary.title())
                        .ticketsSold(salesSummary.ticketsSold())
                        .revenue(salesSummary.revenue())
                        .build())
                .toList();
    }
}
