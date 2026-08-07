package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "manage", description = "Manage the application and settings")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("manage")
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementController {
  private static final String ARMADILLO_VERSION = "ARMADILLO_VERSION";
  private final ManagementService managementService;
  private final AuditEventPublisher auditor;
  private static final Gson gson = new Gson();

  public ManagementController(ManagementService managementService, AuditEventPublisher auditor) {
    this.managementService = requireNonNull(managementService);
    this.auditor = auditor;
  }

  @Operation(
      summary = "Soft restart armadillo. This will programmatically restart the application.")
  @PostMapping("app/restart/soft")
  public void softRestart(Principal principal) {
    auditor.audit(managementService::softRestartApplication, principal, TRIGGER_SOFT_RESTART);
  }

  @Operation(
      summary =
          "Hard restart armadillo. This will trigger a script that kills armadillo, after which it will startup again.")
  @PostMapping("app/restart/hard")
  public void hardRestart(Principal principal) {
    auditor.audit(
        () -> {
          try {
            managementService.hardRestartApplication();
          } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        TRIGGER_HARD_RESTART);
  }

  @Operation(summary = "Update armadillo version")
  @PostMapping("app/update")
  public void update(Principal principal, String version) {
    auditor.audit(
        () -> {
          try {
            managementService.triggerUpdate(version);
          } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        UPDATE_ARMADILLO,
        Map.of(ARMADILLO_VERSION, version));
  }

  @Operation(summary = "List all available jars")
  @GetMapping("app/list")
  public Set<String> listLocallyAvailableJars(Principal principal) {
    return auditor.audit(
        managementService::listLocallyAvailableJars, principal, LIST_AVAILABLE_VERSIONS);
  }

  @Operation(summary = "Get current OIDC config")
  @GetMapping("auth/oidc-config")
  public Map<String, String> getOidcConfig(Principal principal) {
    return auditor.audit(managementService::getCurrentOidcConfig, principal, GET_OIDC_CONFIG);
  }

  @Operation(summary = "Delete an unused jar")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Jar deleted"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  // this is intended behaviour
  @java.lang.SuppressWarnings({"java:S2083"})
  @DeleteMapping("app/delete-jar")
  public void deleteJar(Principal principal, String version) {
    auditor.audit(
        () -> {
          try {
            managementService.deleteJar(version);
          } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        DELETE_JAR,
        Map.of("VERSION_TO_DELETE", version));
  }

  @Operation(summary = "Get info of latest release")
  @GetMapping("app/latest-release-info")
  public Object getLastReleaseInfo(Principal principal) {
    return auditor.audit(
        () -> {
          try {
            return gson.fromJson(managementService.getLastRelease().toString(), Map.class);
          } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, e.getMessage() + ". Thread was interrupted.");
          }
        },
        principal,
        GET_RELEASE_VERSION);
  }

  @Operation(summary = "Download specified armadillo version")
  @GetMapping(value = "app/download", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter downloadVersion(Principal principal, String version) {
    // Audit the initiation, not the whole stream
    auditor.audit(() -> null, principal, DOWNLOAD_ARMADILLO, Map.of(ARMADILLO_VERSION, version));
    return managementService.downloadArmadilloJar(version.replace("v", ""));
  }

  @Operation(summary = "Download update script")
  @PostMapping(value = "updater/download")
  @ResponseStatus(HttpStatus.CREATED)
  public void downloadUpdateScript(Principal principal, String armadilloVersion) {
    // Audit the initiation, not the whole stream
    auditor.audit(
        () -> null, principal, DOWNLOAD_UPDATE_SCRIPT, Map.of(ARMADILLO_VERSION, armadilloVersion));
    try {
      managementService.downloadUpdateScript(armadilloVersion);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
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
        () -> {
          try {
            managementService.saveNewOidcConfig(oidcDetails);
          } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        UPDATE_OIDC_CONFIG,
        Map.of("OIDC_DETAILS", oidcDetails));
  }
}
