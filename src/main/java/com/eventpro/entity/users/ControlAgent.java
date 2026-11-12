package com.eventpro.entity.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity @Table(name = "control_agents")
@SuperBuilder
@Getter @Setter @NoArgsConstructor
public class ControlAgent extends User {

}