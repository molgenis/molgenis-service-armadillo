package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_PROFILES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.UPSERT_PROFILE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "profiles", description = "API to manage DataSHIELD profiles")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("ds-profiles")
public class ProfilesController {

  private final ProfileService profiles;
  private final AuditEventPublisher auditor;

  public ProfilesController(ProfileService profileService, AuditEventPublisher auditor) {
    this.profiles = profileService;
    this.auditor = auditor;
  }

  @Operation(summary = "List profiles")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All profiles listed",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ProfileConfig.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ProfileConfig> profileList(Principal principal) {
    return auditor.audit(
        () ->
            profiles.profileList().stream()
                .map(profile -> profiles.profileByName(profile.getName()))
                .toList(),
        principal,
        LIST_PROFILES,
        Map.of());
  }

  @Operation(summary = "Get profile by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile listed",
            content = @Content(schema = @Schema(implementation = ProfileConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Profile does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(value = "{name}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public ProfileConfig profileGetByProfileName(Principal principal, @PathVariable String name) {
    return auditor.audit(
        () -> profiles.profileByName(name), principal, GET_PROFILE, Map.of(PROFILE, name));
  }

  @Operation(summary = "Add or update profile (if enabled, including docker image)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void profileUpsert(Principal principal, @RequestBody ProfileConfig profileConfig)
      throws InterruptedException {
    profiles.profileUpsert(profileConfig);
    auditor.audit(
        () -> {}, // because of the exception that might happen, and cannot be caught
        principal,
        UPSERT_PROFILE,
        Map.of(PROFILE, profileConfig));
  }

  @Operation(summary = "Delete profile (if enabled, including docker image)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "Profile does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "{name}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void profileDelete(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> profiles.profileDelete(name), principal, DELETE_PROFILE, Map.of(PROFILE, name));
  }
}
