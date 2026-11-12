package com.eventpro.mapper;

import com.eventpro.dto.response.users.ClientResponse;
import com.eventpro.dto.response.users.OrganizerResponse;
import com.eventpro.dto.response.users.UserSummaryResponse;
import com.eventpro.entity.users.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "role", expression = "java(roleOf(user))")
    UserSummaryResponse toSummary(User user);

    ClientResponse toClient(Client client);

    OrganizerResponse toOrganizer(Organizer organizer);

    default String roleOf(User user) {
        return switch (user) {
            case Client _ -> "CLIENT";
            case Organizer _ -> "ORGANIZER";
            case ControlAgent _ -> "AGENT";
            case Administrator _ -> "ADMIN";
            default -> throw new IllegalStateException("Unknown user type: " + user.getClass());
        };
    }
}
