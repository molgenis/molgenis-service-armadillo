package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.security.AccessStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
public class AccessController {

  private AccessStorageService accessStorageService;
  private AuditEventPublisher auditEventPublisher;
  @Autowired private OAuth2AuthorizedClientService clientService;

  public OAuth2AuthorizedClient getClient() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
    return clientService.loadAuthorizedClient(
        oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
  }

  public AccessController(
      AccessStorageService accessStorageService, AuditEventPublisher auditEventPublisher) {
    this.accessStorageService = accessStorageService;
    this.auditEventPublisher = auditEventPublisher;
  }

  @Operation(summary = "Get raw information from the user")
  @GetMapping("/myPrincipal")
  public AbstractAuthenticationToken getPrincipal(
      Principal principal,
      @RegisteredOAuth2AuthorizedClient("molgenis") OAuth2AuthorizedClient authorizedClient) {
    return (AbstractAuthenticationToken) principal;
  }

  @Operation(summary = "Get my token")
  @GetMapping("/myToken")
  public String getToken(Principal principal) {
    return getClient().getAccessToken().getTokenValue();
  }

  @Operation(
      summary = "Get access permissions",
      description = "List users that have access per project")
  @GetMapping(value = "/access", produces = APPLICATION_JSON_VALUE)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Found the permissions",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Map.class),
                  examples =
                      @ExampleObject(
                          "{\n"
                              + "  \"myproject\": [\n"
                              + "    \"m.a.swertz@gmail.com\"\n"
                              + "  ]\n"
                              + "}"))
            }),
        @ApiResponse(responseCode = "403", description = "Permission denied", content = @Content)
      })
  public Map<String, Set<String>> getAccessList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_ACCESS, Map.of());
    return accessStorageService.getAllPermissionsReadonly();
  }

  @Operation(
      summary = "Grant access",
      description =
          "Grant access to email on one project. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @PostMapping(value = "/access", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(CREATED)
  public void grantAccess(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    accessStorageService.grantEmailToProject(email, project);
    auditEventPublisher.audit(principal, GRANT_ACCESS, Map.of("project", project, "email", email));
  }

  @Operation(summary = "Revoke access", description = "Revoke access from email on one project")
  @DeleteMapping(value = "/access", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void revokeAccess(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    accessStorageService.revokeEmailFromProject(email, project);
    auditEventPublisher.audit(principal, REVOKE_ACCESS, Map.of("project", project, "email", email));
  }

  @Operation(summary = "Get info on current user", description = "Get information on current user")
  @GetMapping(value = "/myAccess", produces = APPLICATION_JSON_VALUE)
  public List<String> getProjectsCurrentUser(Principal principal) {
    Collection<SimpleGrantedAuthority> authorities =
        (Collection<SimpleGrantedAuthority>)
            SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    return authorities.stream()
        .filter(authority -> authority.getAuthority().endsWith("_RESEARCHER"))
        .map(authority -> authority.getAuthority().replace("_RESEARCHER", "").replace("ROLE_", ""))
        .collect(Collectors.toList());
  }
}
