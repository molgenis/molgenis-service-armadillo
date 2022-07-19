package org.molgenis.armadillo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileManagerController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import(AuditEventPublisher.class)
public class ProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    AuditEventPublisher auditEventPublisher;
    MockHttpSession session = new MockHttpSession();
    private String sessionId;
    private final Instant instant = Instant.now();
    @MockBean
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock(lenient = true)
    private Clock clock;

    @BeforeEach
    public void setup() {
        auditEventPublisher.setClock(clock);
        auditEventPublisher.setApplicationEventPublisher(applicationEventPublisher);
        when(clock.instant()).thenReturn(instant);
        sessionId = session.changeSessionId();
    }

    @Test
    @WithMockUser(roles = "SU")
    public void createAndList() throws Exception {
        //list
        mockMvc
                .perform(get("/profile-manager"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON)).andDo(handler -> System.out.println("list profiles returned: " + handler.getResponse().getContentAsString()));

        //delete if exists
        mockMvc
                .perform(delete("/profile-manager").param("profileName", "armadillo"))
                .andExpect(status().isOk()).andDo(handler -> System.out.println("delete profile returned: " + handler.getResponse().getContentAsString()));

        mockMvc
                .perform(delete("/profile-manager").param("profileName", "exposome"))
                .andExpect(status().isOk()).andDo(handler -> System.out.println("delete profile returned: " + handler.getResponse().getContentAsString()));

        //check listing is empty
        mockMvc
                .perform(get("/profile-manager"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("armadillo"))));

        //post new profiles
        mockMvc
                .perform(post("/profile-manager").param("name", "armadillo").param("image", "datashield/armadillo-rserver:2.0.0").param("port", "6312"))
                .andExpect(status().isOk()).andDo(handler -> System.out.println("post profile returned: " + handler.getResponse().getContentAsString()));

        //check listing contains armadillo
        mockMvc
                .perform(get("/profile-manager"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("armadillo")));
    }
}
