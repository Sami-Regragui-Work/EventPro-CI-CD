package com.eventpro.controller.dashboards;

import com.eventpro.config.ClockConfig;
import com.eventpro.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerDashboardController.class)
@Import(ClockConfig.class)
class OrganizerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService service;

    @Test
    void salesReturnsOneSummaryPerEvent() throws Exception {
        given(this.service.organizerSales(1L)).willReturn(List.of(
                new DashboardService.EventSalesSummary(1L, "Conference", 42, new BigDecimal("2100.00")),
                new DashboardService.EventSalesSummary(2L, "Workshop", 3, new BigDecimal("150.00"))
        ));

        this.mockMvc.perform(get("/api/organizers/1/dashboard/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventId").value(1))
                .andExpect(jsonPath("$[0].title").value("Conference"))
                .andExpect(jsonPath("$[0].ticketsSold").value(42))
                .andExpect(jsonPath("$[0].revenue").value(2100.00))
                .andExpect(jsonPath("$[1].title").value("Workshop"));
    }

    @Test
    void salesForOrganizerWithoutEventsReturnsEmptyList() throws Exception {
        given(this.service.organizerSales(9L)).willReturn(List.of());

        this.mockMvc.perform(get("/api/organizers/9/dashboard/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
