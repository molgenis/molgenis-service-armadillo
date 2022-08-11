package org.molgenis.armadillo.controller;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DatashieldProfileController.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import(AuditEventPublisher.class)
public class DatashieldProfileControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired AuditEventPublisher auditEventPublisher;

  MockHttpSession session = new MockHttpSession();
  private String sessionId;
  private final Instant instant = Instant.now();
  @MockBean private ApplicationEventPublisher applicationEventPublisher;

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
    // list
    mockMvc
        .perform(get("/profileConfigs"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andDo(
            handler ->
                System.out.println(
                    "list profiles returned: " + handler.getResponse().getContentAsString()));

    // delete if exists
    mockMvc
        .perform(delete("/profileConfigs/armadillo"))
        .andExpect(status().isOk())
        .andDo(
            handler ->
                System.out.println(
                    "delete profile returned: " + handler.getResponse().getContentAsString()));

    mockMvc
        .perform(delete("/profileConfigs/exposome"))
        .andExpect(status().isOk())
        .andDo(
            handler ->
                System.out.println(
                    "delete profile returned: " + handler.getResponse().getContentAsString()));

    // check listing is empty
    mockMvc
        .perform(get("/profileConfigs"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("armadillo"))));

    // post new profiles
    ProfileConfigProps props = new ProfileConfigProps();
    props.setName("armadillo");
    props.setDockerImage("datashield/armadillo-rserver:2.0.0");
    props.setPort(6312);
    mockMvc
        .perform(
            put("/profileConfigs").content(new Gson().toJson(props)).contentType(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            handler ->
                System.out.println(
                    "post profile returned: " + handler.getResponse().getContentAsString()));

    // check listing contains armadillo
    mockMvc
        .perform(get("/profileConfigs"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("armadillo")));

    // delete if exists
    mockMvc
        .perform(delete("/profileConfigs/armadillo"))
        .andExpect(status().isOk())
        .andDo(
            handler ->
                System.out.println(
                    "delete profile returned: " + handler.getResponse().getContentAsString()));

    // check listing contains armadillo
    mockMvc
        .perform(get("/profileConfigs"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("armadillo"))));
  }
}
