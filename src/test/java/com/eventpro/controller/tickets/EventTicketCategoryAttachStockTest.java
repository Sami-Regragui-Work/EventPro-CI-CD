package com.eventpro.controller.tickets;

import com.eventpro.config.ClockConfig;
import com.eventpro.entity.tickets.EventTicketCategory;
import com.eventpro.mapper.EventTicketCategoryMapperImpl;
import com.eventpro.service.EventTicketCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the attach endpoint with the real MapStruct mapper so the serialized
 * stock is checked, not a mapper mock (the mocked slice test accepts any shape).
 */
@WebMvcTest(EventTicketCategoryController.class)
@Import({ClockConfig.class, EventTicketCategoryMapperImpl.class})
class EventTicketCategoryAttachStockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventTicketCategoryService service;

    @Test
    void attachReturnsTheDerivedStockInsteadOfUnmappedZero() throws Exception {
        EventTicketCategory freshCategory = EventTicketCategory.builder()
                .id(10L)
                .totalQuantity(50)
                .maxQuotaPerClient(4)
                .build();
        given(this.service.attach(eq(1L), eq(1L), any()))
                .willReturn(new EventTicketCategoryService.CategoryStock(freshCategory, 0L, 0L, 50L));

        this.mockMvc.perform(post("/api/organizers/1/events/1/ticket-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"VIP\",\"unitPrice\":50,\"totalQuantity\":50,\"maxQuotaPerClient\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.totalQuantity").value(50))
                .andExpect(jsonPath("$.reservedQuantity").value(0))
                .andExpect(jsonPath("$.soldQuantity").value(0))
                .andExpect(jsonPath("$.availableQuantity").value(50));
    }
}
