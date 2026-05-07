package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
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
import java.io.FileNotFoundException;
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
  private final ManagementService managementService;
  private final AuditEventPublisher auditor;

  public ManagementController(ManagementService managementService, AuditEventPublisher auditor) {
    this.managementService = requireNonNull(managementService);
    this.auditor = auditor;
  }

  @Operation(summary = "Soft restart armadillo")
  @PostMapping("app/restart/soft")
  public void softRestart(Principal principal) {
    auditor.audit(managementService::softRestartApplication, principal, "TRIGGER_SOFT_RESTART");
  }

  @Operation(summary = "Hard restart armadillo")
  @PostMapping("app/restart/hard")
  public void hardRestart(Principal principal) {
    auditor.audit(managementService::hardRestartApplication, principal, "TRIGGER_HARD_RESTART");
  }

  // TODO: not sure if we need this or do everything separately in UI?
  //  @Operation(summary = "Update armadillo version")
  //  @PostMapping("app/update")
  //  public void update(Principal principal, @RequestBody OidcDetails oidcDetails, String version)
  // {
  //    auditor.audit(
  //        () -> {
  //          try {
  //            managementService.triggerUpdate(oidcDetails, version);
  //          } catch (FileNotFoundException e) {
  //            throw new ResponseStatusException(
  //                HttpStatus.BAD_REQUEST, e.getMessage() + ": directory doesn't exist.");
  //          } catch (IOException | InterruptedException e) {
  //            throw new RuntimeException(e);
  //          }
  //        },
  //        principal,
  //        "UPDATE_ARMADILLO");
  //  }

  @Operation(summary = "Check if armadillo update is available")
  @GetMapping("app/check-update")
  public boolean checkIfUpdateAvailable(Principal principal) {
    return auditor.audit(
        () -> {
          try {
            return managementService.isArmadilloUpdateAvailable();
          } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        "CHECK_UPDATE");
  }

  @Operation(summary = "List all available jars")
  @GetMapping("app/list")
  public Set<String> listAvailable(Principal principal) {
    return auditor.audit(
        managementService::listAvailableJars, principal, "LIST_AVAILABLE_VERSIONS");
  }

  @Operation(summary = "Get current OIDC config")
  @GetMapping("auth/oidc-config")
  public Map<String, String> getOidcConfig(Principal principal) {
    return auditor.audit(managementService::getCurrentOidcConfig, principal, "GET_OIDC_CONFIG");
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
  @DeleteMapping("app/delete-jar")
  public void listAvailable(Principal principal, String version) {
    auditor.audit(
        () -> {
          try {
            managementService.deleteJar(version);
          } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        "DELETE_JAR",
        Map.of("VERSION_TO_DELETE", version));
  }

  @Operation(summary = "Get info of latest release")
  @GetMapping("app/latest-release-info")
  public Map getLastReleaseInfo(Principal principal) {
    return auditor.audit(
        () -> {
          try {
            Gson gson = new Gson();
            return gson.fromJson(managementService.getLastRelease().toString(), Map.class);
          } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        "GET_RELEASE_VERSION");
  }

  @Operation(summary = "Download specified armadillo version")
  @GetMapping(value = "app/download/version", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter downloadVersion(Principal principal, String version) {
    try {
      // Audit the initiation, not the whole stream
      auditor.audit(
          () -> null, principal, "DOWNLOAD_ARMADILLO", Map.of("ARMADILLO_VERSION", version));
      return managementService.downloadArmadilloJar(version.replace("v", ""));
    } catch (IOException | InterruptedException e) {
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
  // TODO: will it ever finish?
  public void oidcUpsert(Principal principal, @RequestBody OidcDetails oidcDetails) {
    auditor.audit(
        () -> {
          try {
            managementService.saveNewOidcConfig(oidcDetails);
          } catch (FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        "UPDATE_OIDC_CONFIG",
        Map.of("OIDC_DETAILS", oidcDetails));
  }
}
