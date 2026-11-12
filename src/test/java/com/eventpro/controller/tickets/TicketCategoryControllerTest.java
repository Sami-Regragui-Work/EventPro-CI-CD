package com.eventpro.controller.tickets;

import com.eventpro.config.ClockConfig;
import com.eventpro.dto.response.tickets.TicketCategoryResponse;
import com.eventpro.entity.tickets.TicketCategory;
import com.eventpro.mapper.TicketCategoryMapper;
import com.eventpro.service.TicketCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketCategoryController.class)
@Import(ClockConfig.class)
class TicketCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketCategoryService service;

    @MockitoBean
    private TicketCategoryMapper mapper;

    private TicketCategory category() {
        return TicketCategory.builder().id(1L).name("VIP").build();
    }

    private TicketCategoryResponse response() {
        return TicketCategoryResponse.builder().id(1L).name("VIP").build();
    }

    @Test
    void getAllReturnsEveryCategory() throws Exception {
        TicketCategory category = this.category();
        given(this.service.findAll()).willReturn(List.of(category));
        given(this.mapper.toResponse(category)).willReturn(this.response());

        this.mockMvc.perform(get("/api/ticket-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("VIP"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findOrCreateReturns201WhenCategoryIsNew() throws Exception {
        TicketCategory category = this.category();
        given(this.service.findOrCreate("VIP")).willReturn(new TicketCategoryService.FindOrCreateTicketCategory(category, false));
        given(this.mapper.toResponse(category)).willReturn(this.response());

        this.mockMvc.perform(post("/api/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"VIP\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("VIP"));
    }

    @Test
    void findOrCreateReturns200WhenCategoryAlreadyExists() throws Exception {
        TicketCategory category = this.category();
        given(this.service.findOrCreate("vip  ")).willReturn(new TicketCategoryService.FindOrCreateTicketCategory(category, true));
        given(this.mapper.toResponse(category)).willReturn(this.response());

        this.mockMvc.perform(post("/api/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"vip  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void findOrCreateWithBlankNameReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/ticket-categories"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Category name is required"));
    }
}
