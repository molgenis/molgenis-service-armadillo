package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.START_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.STOP_PROFILE;
import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.Map;
import javax.validation.Valid;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.profile.DockerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "profiles")
@RestController
@Valid
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("ds-profiles")
public class ProfilesDockerController {

  public static final String DOCKER_MANAGEMENT_ENABLED = "armadillo.docker-management-enabled";

  private final DockerService dockerService;
  private final AuditEventPublisher auditor;

  public ProfilesDockerController(DockerService dockerService, AuditEventPublisher auditor) {
    this.dockerService = dockerService;
    this.auditor = auditor;
  }

  @Operation(
      summary = "Start a profile's Docker container",
      description = "This will create a new container, or recreate an existing container.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile started"),
        @ApiResponse(
            responseCode = "404",
            description = "Profile does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "405",
            description = "Profile configuration is incomplete",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Profile's container couldn't be started (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PostMapping("{name}/start")
  @ResponseStatus(NO_CONTENT)
  public void startProfileContainer(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> dockerService.startProfile(name), principal, START_PROFILE, Map.of(PROFILE, name));
  }

  @Operation(summary = "Stop and remove a profile's Docker container")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile container stopped"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Profile's container couldn't be stopped (Docker error)",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PostMapping("{name}/stop")
  @ResponseStatus(NO_CONTENT)
  public void stopProfileContainer(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> dockerService.removeProfile(name), principal, STOP_PROFILE, Map.of(PROFILE, name));
  }
}
