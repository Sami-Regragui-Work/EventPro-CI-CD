package com.eventpro.dto.response.users;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
public class UserSummaryResponse extends UserAbstractResponse{
    private final String role;
}
