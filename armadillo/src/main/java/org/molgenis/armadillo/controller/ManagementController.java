package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.security.OidcConfig;
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
  @GetMapping("auth/oidc-config")
  public OidcConfig getOidcConfig(Principal principal) {
    return auditor.audit(managementService::getOidcConfig, principal, "GET_OIDC_CONFIG");
  }

  @Operation(summary = "Restart armadillo")
  @PostMapping("app/restart")
  public void restart(Principal principal) {
    auditor.audit(managementService::restartApplication, principal, "TRIGGER_RESTART");
  }

  @PostMapping("auth/reload")
  public void reloadAuth(Principal principal) {
    auditor.audit(managementService::reloadOidcRegistration, principal, "RELOAD_OIDC");
  }

  @Operation(summary = "Change the OIDC config")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "OIDC config updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(value = "auth/oidc-config", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void oidcUpsert(Principal principal, @RequestBody OidcDetails oidcDetails) {
    auditor.audit(
        () ->
            managementService.saveNewOidcConfig(
                oidcDetails.getIssuerUri(),
                oidcDetails.getClientId(),
                oidcDetails.getClientSecret()),
        principal,
        UPDATE_OIDC_CONFIG,
        Map.of(OIDC_DETAILS, oidcDetails));
  }
}
