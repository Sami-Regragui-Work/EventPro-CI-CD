package com.eventpro;

import org.springframework.boot.SpringApplication;

public class TestEventproApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(EventproApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
