package com.eventpro.entity.users;

import com.eventpro.entity.reservations.Reservation;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity @Table(name = "clients")
@SuperBuilder
@Getter @Setter @NoArgsConstructor
public class Client extends User {
    private String phone;

    @OneToMany(mappedBy = "client")
    private List<Reservation> reservations;
}
