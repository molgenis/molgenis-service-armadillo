package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "manage", description = "Manage the application and settings")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("manage")
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementController {
  private final ManagementService managementService;
  private final AuditEventPublisher auditor;

  public ManagementController(ManagementService managementService, AuditEventPublisher auditor) {
    this.managementService = requireNonNull(managementService);
    this.auditor = auditor;
  }

  @Operation(summary = "Get oidc client information")
  @GetMapping("auth/oidc-client")
  public Map<String, String> getOidcClient(Principal principal) {
    return auditor.audit(managementService::getClient, principal, "GET_OIDC_CLIENT");
  }

  @Operation(summary = "Restart armadillo")
  @PostMapping("app/restart")
  public void restart(Principal principal) {
    auditor.audit(managementService::restartApplication, principal, "TRIGGER_RESTART");
  }
}
