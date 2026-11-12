package com.eventpro.controller.events;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.EventStatus;
import com.eventpro.dto.response.events.EventResponse;
import com.eventpro.entity.Event;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.EventMapper;
import com.eventpro.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerEventController.class)
@Import(ClockConfig.class)
class OrganizerEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService service;

    @MockitoBean
    private EventMapper mapper;

    private static final String VALID_CREATE_BODY = """
            {"title":"Conference","location":"Casablanca","startDate":"2026-06-01T09:00:00Z",
             "salesOpeningDate":"2026-01-01T00:00:00Z","salesClosingDate":"2026-05-31T23:59:00Z","capacity":100}
            """;

    private Event event() {
        return Event.builder().id(1L).title("Conference").build();
    }

    private EventResponse response(EventStatus status) {
        return EventResponse.builder().id(1L).title("Conference").status(status).organizerId(1L).capacity(100).build();
    }

    @Test
    void createReturns201WithMappedEvent() throws Exception {
        Event event = this.event();
        given(this.service.create(eq(1L), any())).willReturn(event);
        given(this.mapper.toResponse(event)).willReturn(this.response(EventStatus.DRAFT));

        this.mockMvc.perform(post("/api/organizers/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Conference"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createWithMissingTitleReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\":\"Casablanca\",\"startDate\":\"2026-06-01T09:00:00Z\",\"salesOpeningDate\":\"2026-01-01T00:00:00Z\",\"salesClosingDate\":\"2026-05-31T23:59:00Z\",\"capacity\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/organizers/1/events"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"));
    }

    @Test
    void createWithZeroCapacityReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Conference\",\"location\":\"Casablanca\",\"startDate\":\"2026-06-01T09:00:00Z\",\"salesOpeningDate\":\"2026-01-01T00:00:00Z\",\"salesClosingDate\":\"2026-05-31T23:59:00Z\",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.capacity").value("Capacity must be at least 1"));
    }

    @Test
    void listReturnsPagedResponses() throws Exception {
        Event event = this.event();
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("startDate"));
        given(this.service.findByOrganizer(1L, pageable)).willReturn(new PageImpl<>(List.of(event), pageable, 1));
        given(this.mapper.toResponse(event)).willReturn(this.response(EventStatus.DRAFT));

        this.mockMvc.perform(get("/api/organizers/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Conference"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void publishReturns200WithPublishedEvent() throws Exception {
        Event published = this.event();
        given(this.service.publish(1L, 1L)).willReturn(published);
        given(this.mapper.toResponse(published)).willReturn(this.response(EventStatus.PUBLISHED));

        this.mockMvc.perform(post("/api/organizers/1/events/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishWhenEventUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Event 9 not found for organizer 1")).given(this.service).publish(anyLong(), anyLong());

        this.mockMvc.perform(post("/api/organizers/1/events/9/publish"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event 9 not found for organizer 1"))
                .andExpect(jsonPath("$.path").value("/api/organizers/1/events/9/publish"));
    }

    @Test
    void patchReturns200WithUpdatedEvent() throws Exception {
        Event updated = this.event();
        given(this.service.update(eq(1L), eq(1L), any())).willReturn(updated);
        given(this.mapper.toResponse(updated)).willReturn(this.response(EventStatus.DRAFT));

        this.mockMvc.perform(patch("/api/organizers/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Conference v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Conference"));
    }

    @Test
    void patchWithBlankTitleReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(patch("/api/organizers/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title cannot be blank"));
    }
}
