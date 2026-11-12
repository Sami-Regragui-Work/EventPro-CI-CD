package com.eventpro.controller.transactions;

import com.eventpro.dto.request.CreatePromotionRequest;
import com.eventpro.dto.response.PromotionResponse;
import com.eventpro.exception.badrequest.ValidationException;
import com.eventpro.mapper.PromotionMapper;
import com.eventpro.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizers/{organizerId}/events/{eventId}/promotions")
@RequiredArgsConstructor
public class PromotionController {
    private final PromotionService service;
    private final PromotionMapper mapper;

    @GetMapping
    public List<PromotionResponse> list(
            @PathVariable Long organizerId,
            @PathVariable Long eventId
    )
    {
        return this.service.list(organizerId, eventId).stream().map(this.mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(
            @PathVariable Long organizerId,
            @PathVariable Long eventId,
            @Valid @RequestBody CreatePromotionRequest dto
    ) throws ValidationException
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.mapper.toResponse(this.service.create(organizerId, eventId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long organizerId,
            @PathVariable Long eventId,
            @PathVariable Long id
    )
    {
        this.service.delete(organizerId, eventId, id);
        return ResponseEntity.noContent().build();
    }
}
