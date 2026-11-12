package com.eventpro.controller.dashboards;

import com.eventpro.dto.response.PageResponse;
import com.eventpro.dto.response.reservations.ReservationResponse;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final DashboardService service;
    private final ReservationMapper reservationMapper;

    @GetMapping("/reservations")
    public PageResponse<ReservationResponse> reservations(
            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        return PageResponse.of(this.service.reservationHistory(pageable).map(this.reservationMapper::toResponse));
    }
}
