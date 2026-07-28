package org.molgenis.armadillo.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.armadillo.service.RebootScriptRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class NewManagementControllerTest {

  @TempDir static java.nio.file.Path storageDir;

  @DynamicPropertySource
  static void storageProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.root-dir", () -> storageDir.toString());
  }

  @Autowired MockMvc mockMvc;

  @TestBean(methodName = "rebootScriptRunnerOverride")
  RebootScriptRunner rebootScriptRunner;

  private static RebootScriptRunner rebootScriptRunnerOverride() {
    return new RecordingRebootScriptRunner();
  }

  @Test
  void getOidcConfig_GET() throws Exception {
    mockMvc
        .perform(get("/manage/auth/oidc-config").with(httpBasic("admin", "password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.issuerUri").value("http://auth-server"))
        .andExpect(jsonPath("$.clientId").value("client-id"))
        .andExpect(jsonPath("$.clientSecret").value("this-is-very-secret"))
        .andExpect(jsonPath("$.deviceClientId").value("device-client-id"))
        .andExpect(jsonPath("$.deviceIssuerUri").doesNotExist());
  }

  @Test
  void getOidcConfig_GET_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(get("/manage/auth/oidc-config").with(httpBasic("someone-else", "wrong-password")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getOidcConfig_GET_unauthenticated() throws Exception {
    mockMvc.perform(get("/manage/auth/oidc-config")).andExpect(status().isUnauthorized());
  }

  @Test
  void softRestart_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void softRestart_POST() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()).with(httpBasic("admin", "password")))
        .andExpect(status().isOk());

    await()
        .atMost(5, SECONDS)
        .until(
            () -> !((RecordingRebootScriptRunner) rebootScriptRunner).getRecordedRuns().isEmpty());
  }

  static class RecordingRebootScriptRunner implements RebootScriptRunner {

    private final List<String[]> runconfigs = new ArrayList<>();

    @Override
    public void runRebootScript(String... args) {
      runconfigs.add(args);
    }

    public List<String[]> getRecordedRuns() {
      return runconfigs;
    }
  }
}
