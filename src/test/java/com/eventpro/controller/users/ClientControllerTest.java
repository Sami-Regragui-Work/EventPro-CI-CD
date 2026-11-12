package com.eventpro.controller.users;

import com.eventpro.dto.response.users.ClientResponse;
import com.eventpro.entity.users.Client;
import com.eventpro.exception.notfound.ResourceNotFoundException;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private UserMapper mapper;

    // The error handler stamps responses from the clock; a fixed whole-second clock
    // makes the serialized timestamp predictable for the shape assertion below.
    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    void getReturnsClientDetails() throws Exception {
        Client client = new Client();
        client.setId(1L);
        client.setName("Amina Karimi");
        client.setEmail("amina@example.com");
        client.setPhone("+212600000000");
        given(this.service.getClient(1L)).willReturn(client);
        given(this.mapper.toClient(client)).willReturn(
                ClientResponse.builder().id(1L).name("Amina Karimi").email("amina@example.com").phone("+212600000000").build());

        this.mockMvc.perform(get("/api/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amina Karimi"))
                .andExpect(jsonPath("$.phone").value("+212600000000"));
    }

    @Test
    void getWhenClientUnknownReturns404Shape() throws Exception {
        given(this.service.getClient(9L)).willThrow(new ResourceNotFoundException("Client 9 not found"));

        this.mockMvc.perform(get("/api/clients/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Client 9 not found"))
                .andExpect(jsonPath("$.path").value("/api/clients/9"))
                .andExpect(jsonPath("$.timestamp").value("2026-06-15T10:00:00Z"));
    }
}
