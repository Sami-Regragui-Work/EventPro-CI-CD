package com.eventpro.entity.users;

import com.eventpro.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity @Table(name = "organizers")
@SuperBuilder
@Getter @Setter @NoArgsConstructor
public class Organizer extends User {
    @Column(nullable = false)
    private String organisationName;

    @OneToMany(mappedBy = "organizer")
    private List<Event> events;
}
