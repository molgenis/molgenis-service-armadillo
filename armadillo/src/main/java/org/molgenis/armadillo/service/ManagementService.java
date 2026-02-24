package org.molgenis.armadillo.service;

import java.util.Map;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {
  public static final String AUDIT_FILE = "AUDIT_FILE";
  public static final String LOG_FILE = "LOG_FILE";

  @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
  private String clientId;

  @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
  private String clientSecret;

  public ManagementService() {}

  public Map<String, String> getClient() {
    return Map.of(
        "client-id", clientId,
        "client-secret", clientSecret);
  }

  public void restartApplictation() {
    ArmadilloServiceApplication.restart();
  }
}
