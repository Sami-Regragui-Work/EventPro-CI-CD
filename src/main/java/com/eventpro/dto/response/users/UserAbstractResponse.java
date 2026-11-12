package com.eventpro.dto.response.users;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
public abstract class UserAbstractResponse {
    private final Long id;
    private final String name;
    private final String email;
}
