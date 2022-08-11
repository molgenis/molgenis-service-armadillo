package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.config.DatashieldProfileManager;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo - profile manager endpoint", version = "0.1.0"),
    security = {
      @SecurityRequirement(name = "JSESSIONID"),
      @SecurityRequirement(name = "http"),
      @SecurityRequirement(name = "jwt")
    })
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@SecurityScheme(name = "jwt", in = HEADER, type = APIKEY)
@RestController
@Validated
public class DatashieldProfileController {
  // audit log
  AuditEventPublisher auditEventPublisher;
  DatashieldProfileManager datashieldProfileManager;

  public DatashieldProfileController(
      AuditEventPublisher auditEventPublisher, DatashieldProfileManager datashieldProfileManager) {
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.datashieldProfileManager = requireNonNull(datashieldProfileManager);
  }

  @Operation(
      summary = "list profiles",
      description = "List currently installed profiles.",
      security = {@SecurityRequirement(name = "jwt")})
  @GetMapping(value = "profile-manager", produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  public List<ProfileConfigProps> listProfiles(Principal principal) {
    this.auditEventPublisher.audit(principal, "List armadillo profiles", Map.of());
    return datashieldProfileManager.listDatashieldProfiles();
  }

  @Operation(
      summary = "add profile",
      description = "Add a new profile",
      security = {@SecurityRequirement(name = "jwt")})
  @PutMapping(value = "profile-manager", produces = TEXT_PLAIN_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  @ResponseStatus(HttpStatus.OK)
  public void addProfile(Principal principal, @RequestBody ProfileConfigProps profileConfigProps)
      throws InterruptedException {
    datashieldProfileManager.addDockerProfile(profileConfigProps);
    this.auditEventPublisher.audit(
        principal,
        "Created armadillo profile: ",
        Map.of("profileName", profileConfigProps.getName()));
  }

  @Operation(
      summary = "remove profile",
      description = "Remove profile",
      security = {@SecurityRequirement(name = "jwt")})
  @DeleteMapping(value = "profile-manager/{profileName}", produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  @ResponseStatus(HttpStatus.OK)
  public void deleteProfile(Principal principal, @PathVariable String profileName) {
    datashieldProfileManager.removeDockerProfile(profileName);
    this.auditEventPublisher.audit(
        principal, "Deleted profile", Map.of("profileName", profileName));
  }
}
