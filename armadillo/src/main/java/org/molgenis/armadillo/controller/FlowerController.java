package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.FLOWER_PUSH_DATA;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.RESOURCE;
import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;
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
import org.molgenis.armadillo.service.FlowerDataService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "flower", description = "Flower federated learning")
@RestController
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("flower")
public class FlowerController {

  private final FlowerDataService flowerDataService;
  private final AuditEventPublisher auditor;

  public FlowerController(FlowerDataService flowerDataService, AuditEventPublisher auditor) {
    this.flowerDataService = flowerDataService;
    this.auditor = auditor;
  }

  @Operation(
      summary = "Push data to a Flower container",
      description =
          "Reads data from Armadillo storage and copies it into the specified container "
              + "at /tmp/armadillo_data/<project>_<resource>.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Data pushed successfully"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user lacks project researcher role",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "404",
            description = "Container or resource not found",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "500",
            description = "Error pushing data to container",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PostMapping("push-data")
  @ResponseStatus(NO_CONTENT)
  public void pushData(Principal principal, @Valid @RequestBody PushDataRequest request) {
    auditor.audit(
        () ->
            flowerDataService.pushData(
                request.project(), request.resource(), request.containerName()),
        principal,
        FLOWER_PUSH_DATA,
        Map.of(
            PROJECT, request.project(),
            RESOURCE, request.resource(),
            CONTAINER, request.containerName()));
  }
}
