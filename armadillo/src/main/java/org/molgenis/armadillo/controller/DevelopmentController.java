package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.getActiveContainerName;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.DockerService;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "developer", description = "API only available for admin users or in container=test")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "JSESSIONID")
@RestController
@Validated
@PreAuthorize("hasRole('ROLE_SU')")
public class DevelopmentController {

  private final Commands commands;
  private final AuditEventPublisher auditEventPublisher;
  private final ContainerService containers;
  private final DockerService dockerService;

  public DevelopmentController(
      Commands commands,
      AuditEventPublisher auditEventPublisher,
      ContainerService containerService,
      @Nullable DockerService dockerService) {
    this.commands = requireNonNull(commands);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.containers = requireNonNull(containerService);
    this.dockerService = dockerService;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Operation(
      summary = "Install a package",
      description = "Install a package in the currently selected container")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object uploaded successfully"),
        @ApiResponse(
            responseCode = "400",
            description =
                "Bad Request: Package installation is only supported for DataSHIELD containers"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
      })
  @PostMapping(
      value = "install-package",
      consumes = {MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public CompletableFuture<ResponseEntity<Void>> installPackage(
      Principal principal, @RequestParam MultipartFile file) {
    String ogFilename = file.getOriginalFilename();
    if (ogFilename == null || ogFilename.isBlank()) {
      Map<String, Object> data = new HashMap<>();
      data.put(MESSAGE, "Filename is null or empty");
      auditEventPublisher.audit(principal, INSTALL_PACKAGES_FAILURE, data);
      return completedFuture(status(INTERNAL_SERVER_ERROR).build());
    } else {

      ContainerConfig activeConfig = containers.getByName(getActiveContainerName());

      if (!(activeConfig instanceof DatashieldContainerConfig)) {
        String msg =
            "Package installation is only supported for DataSHIELD containers. Found type: "
                + activeConfig.getClass().getSimpleName();
        Map<String, Object> data = new HashMap<>();
        data.put(MESSAGE, msg);
        data.put(CONTAINER, activeConfig.getName());
        auditEventPublisher.audit(principal, INSTALL_PACKAGES_FAILURE, data);

        // Return 400 Bad Request
        return completedFuture(status(HttpStatus.BAD_REQUEST).build());
      }

      String filename = file.getOriginalFilename();

      auditEventPublisher.audit(principal, INSTALL_PACKAGES, Map.of(INSTALL_PACKAGES, filename));

      CompletableFuture<Void> result;
      try {
        result =
            commands.installPackage(principal, new ByteArrayResource(file.getBytes()), filename);
      } catch (IOException e) {
        throw new FileProcessingException();
      }

      String packageName = getPackageNameFromFilename(filename);

      return result
          .thenApply(
              body -> {
                containers.addToWhitelist(getActiveContainerName(), packageName);
                return ResponseEntity.ok(body);
              })
          .exceptionally(
              t -> new ResponseEntity(t.getCause().getCause().getMessage(), INTERNAL_SERVER_ERROR));
    }
  }

  @Operation(summary = "Get whitelist")
  @GetMapping("whitelist")
  @ResponseBody
  @PreAuthorize("hasRole('ROLE_SU')")
  public Set<String> getWhitelist() {
    return containers.getPackageWhitelist(getActiveContainerName());
  }

  @Operation(
      summary = "Add package to whitelist",
      description =
          "Adds a package to the runtime whitelist. The whitelist is reset when the application "
              + "restarts. Admin use only.")
  @PostMapping("whitelist/{pkg}")
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public void addToWhitelist(@PathVariable String pkg, Principal principal) {

    String containerName = getActiveContainerName();

    auditEventPublisher.audit(
        () -> containers.addToWhitelist(containerName, pkg),
        principal,
        UPSERT_CONTAINER,
        Map.of(CONTAINER, containerName));
  }

  @Operation(summary = "Delete a docker image", description = "Delete a docker image based on id")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @DeleteMapping(value = "delete-docker-image")
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public void deleteDockerImage(Principal principal, @RequestParam String imageId) {
    assert dockerService != null;
    auditEventPublisher.audit(
        () -> dockerService.deleteImageIfUnused(imageId),
        principal,
        DELETE_DOCKER_IMAGE,
        Map.of(DELETE_DOCKER_IMAGE, imageId));
  }

  protected String getPackageNameFromFilename(String filename) {
    return filename.replaceFirst("_[^_]+$", "");
  }
}
