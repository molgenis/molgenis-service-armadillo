package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.ResponseEntity.status;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "developer", description = "API only available in development mode")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "JSESSIONID")
@RestController
@Validated
@Profile({"development", "test"})
public class DevelopmentController {

  private final Commands commands;
  private final AuditEventPublisher auditEventPublisher;
  private ProfileConfig profileConfig;

  public DevelopmentController(
      Commands commands, AuditEventPublisher auditEventPublisher, ProfileConfig profileConfig) {
    this.commands = requireNonNull(commands);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.profileConfig = requireNonNull(profileConfig);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Operation(
      summary = "Install a package",
      description = "Install a package build from source.",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "install-package")
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
                this.addToWhitelist(packageName);
                return ResponseEntity.ok(body);
              })
          .exceptionally(
              t -> new ResponseEntity(t.getCause().getCause().getMessage(), INTERNAL_SERVER_ERROR));
    }
  }

  @Operation(summary = "Get whitelist")
  @GetMapping("whitelist")
  @ResponseBody
  public Set<String> getWhitelist() {
    return profileConfig.getWhitelist();
  }

  @Operation(
      summary = "Add package to whitelist",
      description =
          "Adds a package to the runtime whitelist. The whitelist is reset when the application "
              + "restarts. Admin use only.")
  @PostMapping("whitelist/{pkg}")
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public void addToWhitelist(@PathVariable String pkg) {
    Set whiteList = new LinkedHashSet();
    whiteList.addAll(profileConfig.getWhitelist());
    whiteList.add(pkg);
    profileConfig =
        ProfileConfig.create(
            profileConfig.getName(),
            profileConfig.getImage(),
            profileConfig.getPort(),
            whiteList,
            profileConfig.getOptions(),
            null);
  }

  protected String getPackageNameFromFilename(String filename) {
    return filename.replaceFirst("_[^_]+$", "");
  }
}
