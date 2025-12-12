package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_CONTAINERS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_CONTAINERS_STATUS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.UPSERT_CONTAINER;
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
import org.molgenis.armadillo.container.*;
import org.molgenis.armadillo.metadata.ContainerService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "containers", description = "API to manage Docker containers")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("containers")
public class ContainersController {

  private final ContainerService containers;
  private final DockerService dockerService;
  private final AuditEventPublisher auditor;
  private final ContainerScheduler containerScheduler;

  public ContainersController(
      ContainerService containerService,
      @Nullable DockerService dockerService,
      AuditEventPublisher auditor,
      ContainerScheduler containerScheduler) {

    this.containers = requireNonNull(containerService);
    this.dockerService = dockerService;
    this.auditor = requireNonNull(auditor);
    this.containerScheduler = requireNonNull(containerScheduler);
  }

  @Operation(
      summary = "List containers",
      description =
          """
                If Docker management is enabled, this will also display each container's Docker
                container status.
                """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All containers listed",
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
  public List<ContainerResponse> containerList(Principal principal) {
    return auditor.audit(this::getContainers, principal, LIST_CONTAINERS);
  }

  @Operation(
      summary = "List containers",
      description =
          """
                        If Docker management is enabled, this will also display each container's status.
                        """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All containers listed",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ContainerResponse.class))))
      })
  @GetMapping(value = "status", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ContainersStatusResponse> getContainerStatus(Principal principal) {
    return auditor.audit(this::getDockerContainerInformation, principal, LIST_CONTAINERS_STATUS);
  }

  private List<ContainersStatusResponse> getDockerContainerInformation() {
    List<ContainerResponse> containers = runAsSystem(this::getContainers);
    List<ContainersStatusResponse> result = new ArrayList<>();
    containers.forEach(
        (container) -> {
          String containerName = container.getName();
          if (dockerService != null) {
            String status =
                runAsSystem(
                    () -> dockerService.getContainerStatus(containerName).getStatus().toString());
            String versions = "";
            if (Objects.equals(status, "RUNNING")) {
              String[] config =
                  runAsSystem(() -> dockerService.getContainerEnvironmentConfig(containerName));
              versions =
                  Arrays.toString(
                      Arrays.stream(config)
                          .filter(configItem -> configItem.contains("_VERSION"))
                          .toArray(String[]::new));
            } else {
              versions = "[]";
            }
            result.add(
                ContainersStatusResponse.create(
                    container.getImage(), containerName, versions, status));
          } else {
            result.add(ContainersStatusResponse.create(container.getImage(), containerName));
          }
        });
    return result;
  }

  private List<ContainerResponse> getContainers() {
    var statuses = new HashMap<String, ContainerInfo>();
    if (dockerService != null) {
      statuses.putAll(dockerService.getAllContainerStatuses());
    }

    return containers.getAll().stream()
        .map(
            container ->
                ContainerResponse.create(
                    container, statuses.getOrDefault(container.getName(), null)))
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
            description = "Container listed",
            content = @Content(schema = @Schema(implementation = ContainerConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Container does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(value = "{name}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public ContainerResponse containerGetByName(Principal principal, @PathVariable String name) {
    return auditor.audit(
        () -> getContainer(name), principal, GET_CONTAINER, Map.of(CONTAINER, name));
  }

  private ContainerResponse getContainer(String name) {
    ContainerInfo container = null;
    if (dockerService != null) {
      container = dockerService.getContainerStatus(name);
    }
    return ContainerResponse.create(containers.getByName(name), container);
  }

  @Operation(summary = "Add or update container")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Container added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void containerUpsert(
      Principal principal, @Valid @RequestBody ContainerConfig containerConfig) {
    auditor.audit(
        () -> {
          containers.upsert(containerConfig); // Save container
          containerScheduler.reschedule(containerConfig); // 🔁 Trigger scheduling
          return null;
        },
        principal,
        UPSERT_CONTAINER,
        Map.of(CONTAINER, containerConfig));
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
        @ApiResponse(responseCode = "204", description = "Container deleted"),
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
            description = "Couldn't remove container (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "{name}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void containerDelete(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> deleteContainer(name), principal, DELETE_CONTAINER, Map.of(CONTAINER, name));
  }

  private void deleteContainer(String name) {
    if (dockerService != null) {
      dockerService.removeContainerDeleteImage(name);
    }
    containers.delete(name);
  }
}
