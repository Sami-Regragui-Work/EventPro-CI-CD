package com.eventpro.controller.tickets;

import com.eventpro.dto.request.tickets.ScanTicketRequest;
import com.eventpro.dto.response.tickets.TicketResponse;
import com.eventpro.entity.tickets.Ticket;
import com.eventpro.mapper.TicketMapper;
import com.eventpro.service.CheckInService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents/{agentId}/checkin")
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService service;
    private final TicketMapper mapper;

    @PostMapping("/scan")
    public ResponseEntity<TicketResponse> scan(
            @PathVariable Long agentId,
            @Valid @RequestBody
            ScanTicketRequest dto
    ) {
        Ticket ticket = this.service.scan(agentId, dto.getCode());
        return ResponseEntity.ok(this.mapper.toResponse(ticket));
    }
}
