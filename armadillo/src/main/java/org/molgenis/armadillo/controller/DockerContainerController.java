package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.START_CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.STOP_CONTAINER;
import static org.molgenis.armadillo.controller.DockerContainerController.DOCKER_MANAGEMENT_ENABLED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.container.DockerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "containers")
@RestController
@Valid
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("/docker/containers")
public class DockerContainerController {

  public static final String DOCKER_MANAGEMENT_ENABLED = "armadillo.docker-management-enabled";
  public static final String DOCKER_RUN_IN_CONTAINER = "armadillo.docker-run-in-container";

  private final DockerService dockerService;
  private final AuditEventPublisher auditor;

  public DockerContainerController(DockerService dockerService, AuditEventPublisher auditor) {
    this.dockerService = dockerService;
    this.auditor = auditor;
  }

  @Operation(
      summary = "Start a Docker container",
      description = "This will create a new container, or recreate an existing container.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Container started"),
        @ApiResponse(
            responseCode = "404",
            description = "Container does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "405",
            description = "Container configuration is incomplete",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Container could not be started (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PostMapping("{name}/start")
  @ResponseStatus(NO_CONTENT)
  public void startContainer(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> dockerService.pullImageStartContainer(name),
        principal,
        START_CONTAINER,
        Map.of(CONTAINER, name));
  }

  @Operation(summary = "Stop and remove a Docker container")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Docker container stopped and removed"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Docker container could not be stopped (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PostMapping("{name}/stop")
  @ResponseStatus(NO_CONTENT)
  public void stopContainer(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> dockerService.stopAndRemoveContainer(name),
        principal,
        STOP_CONTAINER,
        Map.of(CONTAINER, name));
  }
}
