package com.eventpro.controller.users;

import com.eventpro.config.ClockConfig;
import com.eventpro.dto.response.users.UserSummaryResponse;
import com.eventpro.entity.users.Client;
import com.eventpro.mapper.UserMapper;
import com.eventpro.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(ClockConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @MockitoBean
    private UserMapper mapper;

    @Test
    void listReturnsPagedSummaries() throws Exception {
        Client client = new Client();
        client.setId(1L);
        client.setName("Amina Karimi");
        client.setEmail("amina@example.com");
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("id"));
        given(this.service.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(client), pageable, 1));
        given(this.mapper.toSummary(eq(client))).willReturn(
                UserSummaryResponse.builder().id(1L).name("Amina Karimi").email("amina@example.com").role("CLIENT").build());

        this.mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Amina Karimi"))
                .andExpect(jsonPath("$.content[0].role").value("CLIENT"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
