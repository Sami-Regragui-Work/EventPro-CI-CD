package com.eventpro.controller.users;

import com.eventpro.dto.response.users.ClientResponse;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final UserService service;
    private final UserMapper mapper;

    @GetMapping("/{id}")
    public ClientResponse get(@PathVariable Long id) {
        return this.mapper.toClient(this.service.getClient(id));
    }
}
