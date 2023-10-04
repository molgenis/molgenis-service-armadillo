package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
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
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.profile.ContainerInfo;
import org.molgenis.armadillo.profile.DockerService;
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
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("ds-profiles")
public class ProfilesController {

  private final ProfileService profiles;
  private final DockerService dockerService;
  private final AuditEventPublisher auditor;

  public ProfilesController(
      ProfileService profileService,
      @Nullable DockerService dockerService,
      AuditEventPublisher auditor) {
    this.profiles = requireNonNull(profileService);
    this.dockerService = dockerService;
    this.auditor = requireNonNull(auditor);
  }

  @Operation(
      summary = "List profiles",
      description =
          """
                If Docker management is enabled, this will also display each profile's Docker
                container status.
                """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All profiles listed",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ProfileResponse.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ProfileResponse> profileList(Principal principal) {
    return auditor.audit(this::getProfiles, principal, LIST_PROFILES);
  }

  private List<ProfileResponse> getProfiles() {
    var statuses = new HashMap<String, ContainerInfo>();
    if (dockerService != null) {
      statuses.putAll(dockerService.getAllProfileStatuses());
    }

    return profiles.getAll().stream()
        .map(
            profile ->
                ProfileResponse.create(profile, statuses.getOrDefault(profile.getName(), null)))
        .toList();
  }

  @Operation(
      summary = "Get profile by name",
      description =
          """
              If Docker management is enabled, this will also display the profile's Docker
              container status.
              """)
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
  public ProfileResponse profileGetByProfileName(Principal principal, @PathVariable String name) {
    return auditor.audit(() -> getProfile(name), principal, GET_PROFILE, Map.of(PROFILE, name));
  }

  private ProfileResponse getProfile(String name) {
    ContainerInfo container = null;
    if (dockerService != null) {
      container = dockerService.getProfileStatus(name);
    }
    return ProfileResponse.create(profiles.getByName(name), container);
  }

  @Operation(summary = "Add or update profile")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void profileUpsert(Principal principal, @Valid @RequestBody ProfileConfig profileConfig) {
    auditor.audit(
        () -> profiles.upsert(profileConfig),
        principal,
        UPSERT_PROFILE,
        Map.of(PROFILE, profileConfig));
  }

  @Operation(
      summary = "Delete profile",
      description =
          """
              If Docker management is enabled, this will also stop and delete the profile's
              Docker container.
              """)
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile deleted"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "409",
            description = "Attempted to delete default profile",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Couldn't remove profile's container (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "{name}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void profileDelete(Principal principal, @PathVariable String name) {
    auditor.audit(() -> deleteProfile(name), principal, DELETE_PROFILE, Map.of(PROFILE, name));
  }

  private void deleteProfile(String name) {
    if (dockerService != null) {
      dockerService.removeProfile(name);
    }
    profiles.delete(name);
  }
}
