package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.armadillo.audit.AuditEventPublisher.INSTALL_PACKAGES;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.ResponseEntity.status;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo - package endpoint", version = "0.1.0"),
    security = {
        @SecurityRequirement(name = "JSESSIONID"),
        @SecurityRequirement(name = "http"),
        @SecurityRequirement(name = "jwt")
    })
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@SecurityScheme(name = "jwt", in = HEADER, type = APIKEY)
@RestController
@Validated
@Profile({"development", "test"})
public class DevelopmentController {

  private final Commands commands;
  private final AuditEventPublisher auditEventPublisher;
  private final ProfileConfigProps profileConfigProps;

  public DevelopmentController(
      Commands commands,
      AuditEventPublisher auditEventPublisher,
      ProfileConfigProps profileConfigProps) {
    this.commands = requireNonNull(commands);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.profileConfigProps = requireNonNull(profileConfigProps);
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
      Principal principal, @RequestParam MultipartFile file) throws IOException {
    String ogFilename = file.getOriginalFilename();
    if (ogFilename == null || ogFilename.isBlank()) {
      // TODO include error message
      return completedFuture(status(INTERNAL_SERVER_ERROR).build());
    } else {
      String filename = file.getOriginalFilename();

      auditEventPublisher.audit(principal, INSTALL_PACKAGES, Map.of(INSTALL_PACKAGES, filename));
      CompletableFuture<Void> result =
          commands.installPackage(principal, new ByteArrayResource(file.getBytes()), filename);

      String packageName = getPackageNameFromFilename(filename);

      return result
          .thenApply(
              body -> {
                profileConfigProps.addToWhitelist(packageName);
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
    return profileConfigProps.getWhitelist();
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
    profileConfigProps.addToWhitelist(pkg);
  }

  protected String getPackageNameFromFilename(String filename) {
    return filename.replaceFirst("_[^_]+$", "");
  }
}
