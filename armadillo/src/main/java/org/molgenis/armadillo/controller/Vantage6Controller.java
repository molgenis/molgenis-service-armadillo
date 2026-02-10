package org.molgenis.armadillo.controller;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.Vantage6Token;
import org.molgenis.armadillo.service.Vantage6TokenService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "vantage6", description = "Vantage6 integration API for token and access management")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("api/v6")
public class Vantage6Controller {

  private final Vantage6TokenService tokenService;
  private final AuditEventPublisher auditor;

  public Vantage6Controller(Vantage6TokenService tokenService, AuditEventPublisher auditor) {
    this.tokenService = tokenService;
    this.auditor = auditor;
  }

  @Operation(summary = "List all vantage6 access tokens")
  @GetMapping(value = "/tokens", produces = APPLICATION_JSON_VALUE)
  public List<Vantage6Token> listTokens(Principal principal) {
    auditor.audit(principal, "V6_LIST_TOKENS", Map.of());
    return tokenService.getAll();
  }

  @Operation(summary = "Get a vantage6 access token by ID")
  @GetMapping(value = "/tokens/{id}", produces = APPLICATION_JSON_VALUE)
  public Vantage6Token getToken(@PathVariable String id, Principal principal) {
    auditor.audit(principal, "V6_GET_TOKEN", Map.of("tokenId", id));
    return tokenService.getById(id);
  }

  @Operation(summary = "Get authorized projects for a token")
  @GetMapping(value = "/tokens/{id}/permissions", produces = APPLICATION_JSON_VALUE)
  public Set<String> getTokenPermissions(@PathVariable String id, Principal principal) {
    auditor.audit(principal, "V6_GET_TOKEN_PERMISSIONS", Map.of("tokenId", id));
    return tokenService.getAuthorizedProjects(id);
  }

  @Operation(summary = "Create a new vantage6 access token")
  @PostMapping(
      value = "/tokens",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public Vantage6Token createToken(@RequestBody CreateTokenRequest request, Principal principal) {
    auditor.audit(
        principal,
        "V6_CREATE_TOKEN",
        Map.of(
            "containerName", request.containerName(),
            "authorizedProjects", request.authorizedProjects()));
    return tokenService.create(
        request.containerName(), request.authorizedProjects(), request.description());
  }

  @Operation(summary = "Update authorized projects for a token")
  @PutMapping(
      value = "/tokens/{id}/projects",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public Vantage6Token updateTokenProjects(
      @PathVariable String id, @RequestBody Set<String> authorizedProjects, Principal principal) {
    auditor.audit(
        principal,
        "V6_UPDATE_TOKEN_PROJECTS",
        Map.of("tokenId", id, "authorizedProjects", authorizedProjects));
    return tokenService.updateProjects(id, authorizedProjects);
  }

  @Operation(summary = "Delete a vantage6 access token")
  @DeleteMapping("/tokens/{id}")
  @ResponseStatus(NO_CONTENT)
  public void deleteToken(@PathVariable String id, Principal principal) {
    auditor.audit(principal, "V6_DELETE_TOKEN", Map.of("tokenId", id));
    tokenService.delete(id);
  }

  @Operation(summary = "List tokens for a specific container")
  @GetMapping(value = "/containers/{containerName}/tokens", produces = APPLICATION_JSON_VALUE)
  public List<Vantage6Token> getTokensForContainer(
      @PathVariable String containerName, Principal principal) {
    auditor.audit(principal, "V6_LIST_CONTAINER_TOKENS", Map.of("containerName", containerName));
    return tokenService.getByContainerName(containerName);
  }

  public record CreateTokenRequest(
      String containerName, Set<String> authorizedProjects, String description) {}
}
