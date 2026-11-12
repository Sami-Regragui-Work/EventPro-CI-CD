package com.eventpro.controller.users;

import com.eventpro.dto.response.users.OrganizerResponse;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizers")
@RequiredArgsConstructor
public class OrganizerController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping("/{id}")
    public OrganizerResponse get(@PathVariable Long id) {
        return this.mapper.toOrganizer(this.service.getOrganizer(id));
    }
}
