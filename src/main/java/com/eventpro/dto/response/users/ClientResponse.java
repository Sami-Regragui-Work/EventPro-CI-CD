package com.eventpro.dto.response.users;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
public class ClientResponse extends UserAbstractResponse {
    private final String phone;
}
