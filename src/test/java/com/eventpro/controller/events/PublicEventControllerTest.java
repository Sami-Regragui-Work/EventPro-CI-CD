package com.eventpro.controller.events;

import com.eventpro.config.ClockConfig;
import com.eventpro.dto.response.events.EventSummaryResponse;
import com.eventpro.entity.Event;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.service.EventService;
import com.eventpro.service.EventTicketCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
@Import(ClockConfig.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventTicketCategoryService eventTicketCategoryService;

    @MockitoBean
    private EventMapper mapper;

    private Event event() {
        return Event.builder().id(1L).title("Conference").build();
    }

    private EventSummaryResponse summary() {
        return EventSummaryResponse.builder().id(1L).title("Conference").availableSeats(5L).build();
    }

    @Test
    void listReturnsSummariesWithAvailableSeats() throws Exception {
        Event event = this.event();
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("startDate"));
        given(this.eventService.findPublic(pageable))
                .willReturn(new PageImpl<>(List.of(event), pageable, 1));
        given(this.eventTicketCategoryService.availableSeatsByEventId(List.of(1L))).willReturn(Map.of(1L, 5L));
        given(this.mapper.toSummaryResponse(eq(event), eq(5L))).willReturn(this.summary());

        this.mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].availableSeats").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listWithUnknownOrganizerSuffixIsEmpty() throws Exception {
        given(this.eventService.findPublic(any())).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        this.mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getReturnsPublicEventSummary() throws Exception {
        Event event = this.event();
        given(this.eventService.findPublicById(1L)).willReturn(event);
        given(this.mapper.toSummaryResponse(event)).willReturn(summary());

        this.mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.availableSeats").value(5));
    }

    @Test
    void getWhenEventUnknownReturns404Shape() throws Exception {
        given(this.eventService.findPublicById(9L)).willThrow(new ResourceNotFoundException("Event 9 not found"));

        this.mockMvc.perform(get("/api/events/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event 9 not found"))
                .andExpect(jsonPath("$.path").value("/api/events/9"));
    }
}
