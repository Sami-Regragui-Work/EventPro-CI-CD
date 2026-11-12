package com.eventpro.controller.tickets;

import com.eventpro.dto.request.tickets.TicketCategoryRequest;
import com.eventpro.dto.response.tickets.TicketCategoryResponse;
import com.eventpro.mapper.TicketCategoryMapper;
import com.eventpro.service.TicketCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-categories")
@RequiredArgsConstructor
public class TicketCategoryController {
    private final TicketCategoryService service;
    private final TicketCategoryMapper mapper;

    @GetMapping
    public List<TicketCategoryResponse> getAll() {
        return this.service.findAll().stream().map(this.mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TicketCategoryResponse> findOrCreate(
            @Valid @RequestBody TicketCategoryRequest dto
            )
    {
        TicketCategoryService.FindOrCreateTicketCategory result = this.service.findOrCreate(dto.getName());
        TicketCategoryResponse body = this.mapper.toResponse(result.ticketCategory());

        return result.exist()
                ? ResponseEntity.ok(body)
                : ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
