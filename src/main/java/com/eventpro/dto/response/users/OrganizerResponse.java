package com.eventpro.dto.response.users;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter @SuperBuilder
public class OrganizerResponse extends UserAbstractResponse {
    private final String organisationName;
}
