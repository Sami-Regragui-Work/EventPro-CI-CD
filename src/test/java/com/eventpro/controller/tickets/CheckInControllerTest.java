package com.eventpro.controller.tickets;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.TicketStatus;
import com.eventpro.dto.response.tickets.TicketResponse;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.exception.conflict.TicketAlreadyUsedException;
import com.eventpro.exception.conflict.TicketCancelledException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.TicketMapper;
import com.eventpro.service.CheckInService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckInController.class)
@Import(ClockConfig.class)
class CheckInControllerTest {

    private static final String SCAN_PATH = "/api/agents/1/checkin/scan";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInService service;

    @MockitoBean
    private TicketMapper mapper;

    private Ticket ticket() {
        return Ticket.builder().id(1L).code("TKT-B3C9D2").status(TicketStatus.USED).build();
    }

    private TicketResponse response(TicketStatus status) {
        return TicketResponse.builder().id(1L).code("TKT-B3C9D2").status(status).build();
    }

    @Test
    void scanReturns200WithScannedTicket() throws Exception {
        Ticket ticket = this.ticket();
        given(this.service.scan(1L, "TKT-B3C9D2")).willReturn(ticket);
        given(this.mapper.toResponse(ticket)).willReturn(this.response(TicketStatus.USED));

        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TKT-B3C9D2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TKT-B3C9D2"))
                .andExpect(jsonPath("$.status").value("USED"));
    }

    @Test
    void scanWithBlankCodeReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value(SCAN_PATH))
                .andExpect(jsonPath("$.fieldErrors.code").value("Ticket code is required"));
    }

    @Test
    void scanWhenTicketAlreadyUsedReturns409Shape() throws Exception {
        willThrow(new TicketAlreadyUsedException("Ticket TKT-B3C9D2 already used"))
                .given(this.service).scan(any(), any());

        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TKT-B3C9D2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ticket TKT-B3C9D2 already used"))
                .andExpect(jsonPath("$.path").value(SCAN_PATH));
    }

    @Test
    void scanWhenTicketCancelledReturns409Shape() throws Exception {
        willThrow(new TicketCancelledException("Ticket TKT-B3C9D2 is cancelled"))
                .given(this.service).scan(any(), any());

        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TKT-B3C9D2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Ticket TKT-B3C9D2 is cancelled"));
    }

    @Test
    void scanWhenCodeUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Ticket TKT-MISSING not found"))
                .given(this.service).scan(any(), any());

        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TKT-MISSING\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket TKT-MISSING not found"))
                .andExpect(jsonPath("$.path").value(SCAN_PATH));
    }

    @Test
    void scanWhenAgentUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Agent 1 not found"))
                .given(this.service).scan(any(), any());

        this.mockMvc.perform(post(SCAN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TKT-B3C9D2\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Agent 1 not found"))
                .andExpect(jsonPath("$.path").value(SCAN_PATH));
    }
}