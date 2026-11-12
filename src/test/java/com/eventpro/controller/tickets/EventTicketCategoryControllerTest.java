package com.eventpro.controller.tickets;

import com.eventpro.config.ClockConfig;
import com.eventpro.dto.response.tickets.EventTicketCategoryResponse;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.EventTicketCategoryMapper;
import com.eventpro.service.EventTicketCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventTicketCategoryController.class)
@Import(ClockConfig.class)
class EventTicketCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventTicketCategoryService service;

    @MockitoBean
    private EventTicketCategoryMapper mapper;

    private EventTicketCategory category() {
        return EventTicketCategory.builder().id(10L).totalQuantity(100).maxQuotaPerClient(4).build();
    }

    private EventTicketCategoryResponse response() {
        return EventTicketCategoryResponse.builder()
                .id(10L)
                .ticketCategoryId(1L)
                .ticketCategoryName("VIP")
                .unitPrice(new BigDecimal("50.00"))
                .totalQuantity(100)
                .maxQuotaPerClient(4)
                .reservedQuantity(10)
                .soldQuantity(5)
                .availableQuantity(85)
                .build();
    }

    @Test
    void attachReturns201WithMappedCategory() throws Exception {
        EventTicketCategory category = this.category();
        given(this.service.attach(eq(1L), eq(1L), any()))
                .willReturn(new EventTicketCategoryService.CategoryStock(category, 0L, 0L, 100L));
        given(this.mapper.toStockResponse(eq(category), eq(0L), eq(0L), eq(100L)))
                .willReturn(EventTicketCategoryResponse.builder()
                        .id(10L).ticketCategoryId(1L).ticketCategoryName("VIP")
                        .unitPrice(new BigDecimal("50.00"))
                        .totalQuantity(100).maxQuotaPerClient(4)
                        .reservedQuantity(0L).soldQuantity(0L).availableQuantity(100L).build());

        this.mockMvc.perform(post("/api/organizers/1/events/1/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"VIP\",\"unitPrice\":50,\"totalQuantity\":100,\"maxQuotaPerClient\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCategoryName").value("VIP"))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void attachWithMissingFieldsReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events/1/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/organizers/1/events/1/ticket-categories"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Category name is required"))
                .andExpect(jsonPath("$.fieldErrors.unitPrice").value("Unit price is required"))
                .andExpect(jsonPath("$.fieldErrors.totalQuantity").value("Total quantity is required"));
    }

    @Test
    void attachWithNegativePriceReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events/1/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"VIP\",\"unitPrice\":-1,\"totalQuantity\":100,\"maxQuotaPerClient\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.unitPrice").value("Unit price cannot be negative"));
    }

    @Test
    void listReturnsStockOverview() throws Exception {
        EventTicketCategory category = this.category();
        EventTicketCategoryService.CategoryStock stock =
                new EventTicketCategoryService.CategoryStock(category, 10L, 5L, 85L);
        given(this.service.list(1L, 1L)).willReturn(List.of(stock));
        given(this.mapper.toStockResponse(eq(category), eq(10L), eq(5L), eq(85L))).willReturn(this.response());

        this.mockMvc.perform(get("/api/organizers/1/events/1/ticket-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCategoryName").value("VIP"))
                .andExpect(jsonPath("$[0].reservedQuantity").value(10))
                .andExpect(jsonPath("$[0].soldQuantity").value(5))
                .andExpect(jsonPath("$[0].availableQuantity").value(85));
    }

    @Test
    void patchReturns200WithUpdatedStock() throws Exception {
        EventTicketCategory category = this.category();
        given(this.service.update(eq(1L), eq(1L), eq(10L), any()))
                .willReturn(new EventTicketCategoryService.CategoryStock(category, 10L, 5L, 85L));
        given(this.mapper.toStockResponse(eq(category), eq(10L), eq(5L), eq(85L))).willReturn(this.response());

        this.mockMvc.perform(patch("/api/organizers/1/events/1/ticket-categories/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.availableQuantity").value(85));
    }

    @Test
    void patchWhenCategoryUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Ticket category 99 not attached to event 1"))
                .given(this.service).update(anyLong(), anyLong(), anyLong(), any());

        this.mockMvc.perform(patch("/api/organizers/1/events/1/ticket-categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitPrice\":60}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket category 99 not attached to event 1"))
                .andExpect(jsonPath("$.path").value("/api/organizers/1/events/1/ticket-categories/99"));
    }

    @Test
    void removeReturns204() throws Exception {
        this.mockMvc.perform(delete("/api/organizers/1/events/1/ticket-categories/10"))
                .andExpect(status().isNoContent());
    }
}
