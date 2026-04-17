package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.google.gson.JsonElement;
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

  @Operation(summary = "Restart armadillo")
  @PostMapping("app/restart")
  public void restart(Principal principal) {
    auditor.audit(managementService::restartApplication, principal, "TRIGGER_RESTART");
  }

  @Operation(summary = "Update armadillo version")
  @PostMapping("app/update")
  public void update(Principal principal, @RequestBody OidcDetails oidcDetails) {
    auditor.audit(
        () -> {
          try {
            managementService.triggerUpdate(oidcDetails);
          } catch (FileNotFoundException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, e.getMessage() + ": directory doesn't exist.");
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        },
        principal,
        "TRIGGER_UPDATE");
  }

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
        "TRIGGER_UPDATE");
  }

  @Operation(summary = "List all available jars")
  @GetMapping("app/list")
  public Set<String> listAvailable(Principal principal) {
    return auditor.audit(
        managementService::listAvailableJars, principal, "LIST_AVAILABLE_VERSIONS");
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
    auditor.audit(() -> managementService.deleteJar(version), principal, "DELETE_JAR");
  }

  @Operation(summary = "Download latest armadillom")
  @GetMapping(value = "app/download/last", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter downloadLatest(Principal principal) {
    //      const source = new EventSource('/manage/app/download');
    //        source.addEventListener('progress', e => console.log(`${e.data}%`));
    //        source.addEventListener('done', () => source.close());
    try {
      // Audit the initiation, not the whole stream
      auditor.audit(() -> null, principal, "TRIGGER_DOWNLOAD_WITH_PROGRESS");
      JsonElement lastRelease = managementService.getLastRelease();
      String lastVersion = managementService.getReleaseVersion(lastRelease);
      return managementService.downloadArmadilloJar(lastVersion);
    } catch (IOException | InterruptedException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @Operation(summary = "Download specified armadillo version")
  @GetMapping(value = "app/download/last", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter downloadVersion(Principal principal, String version) {
    //      const source = new EventSource('/manage/app/download');
    //        source.addEventListener('progress', e => console.log(`${e.data}%`));
    //        source.addEventListener('done', () => source.close());
    try {
      // Audit the initiation, not the whole stream
      auditor.audit(() -> null, principal, "TRIGGER_DOWNLOAD_WITH_PROGRESS");
      return managementService.downloadArmadilloJar(version);
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
