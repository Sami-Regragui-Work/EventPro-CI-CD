package com.eventpro.controller.dashboards;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.ReservationStatus;
import com.eventpro.dto.response.reservations.ReservationResponse;
import com.eventpro.entity.reservations.Reservation;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import(ClockConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService service;

    @MockitoBean
    private ReservationMapper reservationMapper;

    @Test
    void reservationsReturnsPagedHistory() throws Exception {
        Reservation reservation = Reservation.builder().id(1L).reference("RES-B3C9D2").build();
        given(this.service.reservationHistory(PageRequest.of(0, 20)))
                .willReturn(new PageImpl<>(List.of(reservation), PageRequest.of(0, 20), 1));
        given(this.reservationMapper.toResponse(eq(reservation))).willReturn(
                ReservationResponse.builder()
                        .id(1L)
                        .reference("RES-B3C9D2")
                        .status(ReservationStatus.CONFIRMED)
                        .totalAmount(new BigDecimal("120.00"))
                        .build());

        this.mockMvc.perform(get("/api/admin/dashboard/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").value("RES-B3C9D2"))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void reservationsIsPagedByRequestParameter() throws Exception {
        given(this.service.reservationHistory(any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 5), 0));

        this.mockMvc.perform(get("/api/admin/dashboard/reservations").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
