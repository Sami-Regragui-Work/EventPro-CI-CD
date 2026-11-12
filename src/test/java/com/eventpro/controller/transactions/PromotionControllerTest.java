package com.eventpro.controller.transactions;

import com.eventpro.config.ClockConfig;
import com.eventpro.domain.enums.DiscountType;
import com.eventpro.dto.response.PromotionResponse;
import com.eventpro.entity.transactions.Promotion;
import com.eventpro.exception.conflict.PromotionInUseException;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.PromotionMapper;
import com.eventpro.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromotionController.class)
@Import(ClockConfig.class)
class PromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionService service;

    @MockitoBean
    private PromotionMapper mapper;

    private Promotion promotion() {
        return Promotion.builder()
                .id(1L)
                .code("PROMO10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDate.parse("2026-01-01"))
                .endDate(LocalDate.parse("2026-12-31"))
                .usageLimit(100)
                .build();
    }

    private PromotionResponse response() {
        return PromotionResponse.builder()
                .id(1L)
                .code("PROMO10")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10.00"))
                .startDate(LocalDate.parse("2026-01-01"))
                .endDate(LocalDate.parse("2026-12-31"))
                .usageLimit(100)
                .usageCount(3)
                .build();
    }

    @Test
    void createReturns201WithMappedPromotion() throws Exception {
        Promotion promotion = this.promotion();
        given(this.service.create(eq(1L), eq(1L), any())).willReturn(promotion);
        given(this.mapper.toResponse(promotion)).willReturn(this.response());

        this.mockMvc.perform(post("/api/organizers/1/events/1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discountType\":\"PERCENTAGE\",\"discountValue\":10,\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\",\"usageLimit\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PROMO10"))
                .andExpect(jsonPath("$.usageCount").value(3));
    }

    @Test
    void createWithUnknownDiscountTypeReturns400() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events/1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discountType\":\"MAGIC\",\"discountValue\":10,\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\",\"usageLimit\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void createWithZeroDiscountValueReturns400WithFieldErrors() throws Exception {
        this.mockMvc.perform(post("/api/organizers/1/events/1/promotions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discountType\":\"PERCENTAGE\",\"discountValue\":0,\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\",\"usageLimit\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.discountValue").value("Discount value must be greater than 0"));
    }

    @Test
    void listReturnsPromotions() throws Exception {
        Promotion promotion = this.promotion();
        given(this.service.list(1L, 1L)).willReturn(List.of(promotion));
        given(this.mapper.toResponse(promotion)).willReturn(this.response());

        this.mockMvc.perform(get("/api/organizers/1/events/1/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PROMO10"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        this.mockMvc.perform(delete("/api/organizers/1/events/1/promotions/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteWhenPromotionInUseReturns409Shape() throws Exception {
        willThrow(new PromotionInUseException("Promotion 5 is attached to reservations")).given(this.service).delete(anyLong(), anyLong(), anyLong());

        this.mockMvc.perform(delete("/api/organizers/1/events/1/promotions/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Promotion 5 is attached to reservations"))
                .andExpect(jsonPath("$.path").value("/api/organizers/1/events/1/promotions/5"));
    }

    @Test
    void deleteWhenUnknownReturns404Shape() throws Exception {
        willThrow(new ResourceNotFoundException("Promotion 5 not found")).given(this.service).delete(anyLong(), anyLong(), anyLong());

        this.mockMvc.perform(delete("/api/organizers/1/events/1/promotions/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Promotion 5 not found"));
    }
}
