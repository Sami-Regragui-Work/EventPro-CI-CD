package com.eventpro.controller.users;

import com.eventpro.config.ClockConfig;
import com.eventpro.dto.response.users.OrganizerResponse;
import com.eventpro.entity.users.Organizer;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerController.class)
@Import(ClockConfig.class)
class OrganizerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private UserMapper mapper;

    @Test
    void getReturnsOrganizerDetails() throws Exception {
        Organizer organizer = new Organizer();
        organizer.setId(1L);
        organizer.setName("EventPro Agency");
        organizer.setEmail("agency@example.com");
        organizer.setOrganisationName("EventPro Agency SARL");
        given(this.service.getOrganizer(1L)).willReturn(organizer);
        given(this.mapper.toOrganizer(organizer)).willReturn(
                OrganizerResponse.builder().id(1L).name("EventPro Agency").email("agency@example.com").organisationName("EventPro Agency SARL").build());

        this.mockMvc.perform(get("/api/organizers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("EventPro Agency"))
                .andExpect(jsonPath("$.organisationName").value("EventPro Agency SARL"));
    }

    @Test
    void getWhenOrganizerUnknownReturns404Shape() throws Exception {
        given(this.service.getOrganizer(9L)).willThrow(new ResourceNotFoundException("Organizer 9 not found"));

        this.mockMvc.perform(get("/api/organizers/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Organizer 9 not found"))
                .andExpect(jsonPath("$.path").value("/api/organizers/9"));
    }
}
