package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.molgenis.armadillo.audit.AuditEventPublisher.CONTAINER;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.getActiveContainerName;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
  private final DatashieldContainerConfig datashieldContainerConfig;

  public DevelopmentController(
      Commands commands,
      AuditEventPublisher auditEventPublisher,
      ContainerService containerService,
      @Nullable DockerService dockerService,
      DatashieldContainerConfig datashieldContainerConfig) {
    this.commands = requireNonNull(commands);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.containers = requireNonNull(containerService);
    this.dockerService = dockerService;
    this.datashieldContainerConfig = requireNonNull(datashieldContainerConfig);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @PostMapping(
      value = "install-package",
      consumes = {"multipart/form-data"})
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

      String containerName;
      try {
        containerName = datashieldContainerConfig.getName();
      } catch (IllegalArgumentException e) {
        Map<String, Object> data = new HashMap<>();
        data.put(MESSAGE, e.getMessage());
        data.put(CONTAINER, getActiveContainerName());
        auditEventPublisher.audit(principal, INSTALL_PACKAGES_FAILURE, data);
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
                containers.addToWhitelist(containerName, packageName);
                return ResponseEntity.ok().<Void>build();
              })
          .exceptionally(t -> ResponseEntity.status(INTERNAL_SERVER_ERROR).build());
    }
  }

  @GetMapping("whitelist")
  public Set<String> getWhitelist() {
    return containers.getPackageWhitelist(datashieldContainerConfig.getName());
  }

  @PostMapping("whitelist/{pkg}")
  public void addToWhitelist(@PathVariable String pkg, Principal principal) {
    auditEventPublisher.audit(
        () -> containers.addToWhitelist(datashieldContainerConfig.getName(), pkg),
        principal,
        UPSERT_CONTAINER,
        Map.of(CONTAINER, datashieldContainerConfig.getName()));
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

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
  }
}
