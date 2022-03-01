package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.audit.AuditEventPublisher.INSTALL_PACKAGES;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.Commands;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@Profile("development")
public class PackageController {

  private final Commands commands;
  private final AuditEventPublisher auditEventPublisher;

  public PackageController(Commands commands, AuditEventPublisher auditEventPublisher) {
    this.commands = requireNonNull(commands);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
  }

  @Operation(
      summary = "Install a package",
      description = "Install a package build from source.",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "install-package")
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public CompletableFuture<ResponseEntity<Void>> installPackage(Principal principal, @RequestParam MultipartFile file)
      throws IOException {
    auditEventPublisher.audit(
        principal, INSTALL_PACKAGES, Map.of(INSTALL_PACKAGES, file.getName()));

    CompletableFuture<Void> result = commands.installPackage(
        principal, new ByteArrayResource(file.getBytes()), file.getOriginalFilename());

    return result
        .thenApply(ResponseEntity::ok)
        .exceptionally(t -> new ResponseEntity(t.getMessage(), INTERNAL_SERVER_ERROR));
  }
}
