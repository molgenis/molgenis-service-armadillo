package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_PROFILES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_PROFILES_STATUS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.UPSERT_PROFILE;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
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
import java.util.*;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.container.ContainerInfo;
import org.molgenis.armadillo.container.ContainerScheduler;
import org.molgenis.armadillo.container.DockerService;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
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
public class ContainersController {

  private final ContainerService profiles;
  private final DockerService dockerService;
  private final AuditEventPublisher auditor;
  private final ContainerScheduler containerScheduler;

  public ContainersController(
      ContainerService containerService,
      @Nullable DockerService dockerService,
      AuditEventPublisher auditor,
      ContainerScheduler containerScheduler) {

    this.profiles = requireNonNull(containerService);
    this.dockerService = dockerService;
    this.auditor = requireNonNull(auditor);
    this.containerScheduler = requireNonNull(containerScheduler);
  }

  @Operation(
      summary = "List profiles",
      description =
          """
                If Docker management is enabled, this will also display each container's Docker
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
                        @ArraySchema(schema = @Schema(implementation = ContainerResponse.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ContainerResponse> profileList(Principal principal) {
    return auditor.audit(this::getProfiles, principal, LIST_PROFILES);
  }

  @Operation(
      summary = "List profiles",
      description =
          """
                        If Docker management is enabled, this will also display each container's Docker
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
                        @ArraySchema(schema = @Schema(implementation = ContainerResponse.class))))
      })
  @GetMapping(value = "status", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ContainersStatusResponse> getProfileStatus(Principal principal) {
    return auditor.audit(this::getDockerProfileInformation, principal, LIST_PROFILES_STATUS);
  }

  private List<ContainersStatusResponse> getDockerProfileInformation() {
    List<ContainerResponse> profiles = runAsSystem(this::getProfiles);
    List<ContainersStatusResponse> result = new ArrayList<>();
    profiles.forEach(
        (profile) -> {
          String profileName = profile.getName();
          if (dockerService != null) {
            String status =
                runAsSystem(
                    () -> dockerService.getContainerStatus(profileName).getStatus().toString());
            String versions = "";
            if (Objects.equals(status, "RUNNING")) {
              String[] config =
                  runAsSystem(() -> dockerService.getContainerEnvironmentConfig(profileName));
              versions =
                  Arrays.toString(
                      Arrays.stream(config)
                          .filter(configItem -> configItem.contains("_VERSION"))
                          .toArray(String[]::new));
            } else {
              versions = "[]";
            }
            result.add(
                ContainersStatusResponse.create(profile.getImage(), profileName, versions, status));
          } else {
            result.add(ContainersStatusResponse.create(profile.getImage(), profileName));
          }
        });
    return result;
  }

  private List<ContainerResponse> getProfiles() {
    var statuses = new HashMap<String, ContainerInfo>();
    if (dockerService != null) {
      statuses.putAll(dockerService.getAllContainerStatuses());
    }

    return profiles.getAll().stream()
        .map(
            profile ->
                ContainerResponse.create(profile, statuses.getOrDefault(profile.getName(), null)))
        .toList();
  }

  @Operation(
      summary = "Get container by name",
      description =
          """
              If Docker management is enabled, this will also display the container's Docker
              container status.
              """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile listed",
            content = @Content(schema = @Schema(implementation = ContainerConfig.class))),
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
  public ContainerResponse profileGetByProfileName(Principal principal, @PathVariable String name) {
    return auditor.audit(() -> getProfile(name), principal, GET_PROFILE, Map.of(PROFILE, name));
  }

  private ContainerResponse getProfile(String name) {
    ContainerInfo container = null;
    if (dockerService != null) {
      container = dockerService.getContainerStatus(name);
    }
    return ContainerResponse.create(profiles.getByName(name), container);
  }

  @Operation(summary = "Add or update container")
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
  public void profileUpsert(
      Principal principal, @Valid @RequestBody ContainerConfig containerConfig) {
    auditor.audit(
        () -> {
          profiles.upsert(containerConfig); // Save container
          containerScheduler.reschedule(containerConfig); // 🔁 Trigger scheduling
          return null;
        },
        principal,
        UPSERT_PROFILE,
        Map.of(PROFILE, containerConfig));
  }

  @Operation(
      summary = "Delete container",
      description =
          """
              If Docker management is enabled, this will also stop and delete the container's
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
            description = "Attempted to delete default container",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Couldn't remove container's container (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "{name}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void profileDelete(Principal principal, @PathVariable String name) {
    auditor.audit(() -> deleteProfile(name), principal, DELETE_PROFILE, Map.of(PROFILE, name));
  }

  private void deleteProfile(String name) {
    if (dockerService != null) {
      dockerService.removeContainerDeleteImage(name);
    }
    profiles.delete(name);
  }
}
