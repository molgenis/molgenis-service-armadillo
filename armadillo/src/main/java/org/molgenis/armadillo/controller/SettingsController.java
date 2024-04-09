package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.service.SettingsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "settings", description = "API to manage DataSHIELD profiles")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("settings")
public class SettingsController {
  private final AuditEventPublisher auditor;
  private final SettingsService settingsService;

  public SettingsController(SettingsService settingsService, AuditEventPublisher auditor) {
    this.settingsService = settingsService;
    this.auditor = requireNonNull(auditor);
  }

  @Operation(summary = "Store settings")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Settings stored successfully",
            content = @Content(schema = @Schema(implementation = FileDetails.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(
      path = "properties",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(CREATED)
  public void storeSettings(Principal principal, @RequestBody String settings) throws IOException {
    auditor.audit(
        () -> settingsService.storeSettings(settings),
        principal,
        DOWNLOAD_FILE,
        //            STORE_FILE,
        Map.of("FILE_NAME", "Upsert"));
  }

  @Operation(summary = "Retrieve properties")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File retrieved successfully",
            content = @Content(schema = @Schema(implementation = FileDetails.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "properties", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public String getFile(Principal principal) throws IOException {
    return auditor.audit(
        settingsService::fetchSettings,
        principal,
        DOWNLOAD_FILE,
        //            STORE_FILE,
        Map.of("FILE_NAME", "A"));
  }
}
