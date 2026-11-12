package com.eventpro.controller.users;

import com.eventpro.dto.response.PageResponse;
import com.eventpro.dto.response.users.UserSummaryResponse;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final UserMapper mapper;

    @GetMapping
    public PageResponse<UserSummaryResponse> getUsers(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        // don't forget to work on default behavior without size
        return PageResponse.of(this.service.findAll(pageable).map(this.mapper::toSummary));
    }
}
