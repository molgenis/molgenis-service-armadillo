package org.molgenis.armadillo.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "my", description = "API to inspect properties of current user session")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("my")
public class CurrentUserController {

  private final OAuth2AuthorizedClientService clientService;

  public CurrentUserController(Optional<OAuth2AuthorizedClientService> clientService) {
    this.clientService = clientService.orElse(null);
  }

  @Operation(summary = "Get raw information from the current user")
  @GetMapping("principal")
  public AbstractAuthenticationToken currentUserGetPrincipal(
      Principal principal, final HttpServletResponse response) throws IOException {
    if (principal == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
    return (AbstractAuthenticationToken) principal;
  }

  @Operation(summary = "Token of the current user")
  @GetMapping("token")
  public String currentUserGetToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
      OAuth2AuthorizedClient client =
          clientService.loadAuthorizedClient(
              oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
      return client.getAccessToken().getTokenValue();
    } else if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      return jwtAuthenticationToken.getToken().getTokenValue();
    }
    throw new UnsupportedOperationException("couldn't get token");
  }

  @Operation(
      summary = "Get info on current user",
      description =
          "Get information on current user. Note, if you just gave yourself permission, you need to sign via /logout to refresh permissions")
  @GetMapping(value = "projects", produces = APPLICATION_JSON_VALUE)
  public Set<String> currentUserGetProjects() {
    @SuppressWarnings("unchecked")
    Collection<SimpleGrantedAuthority> authorities =
        (Collection<SimpleGrantedAuthority>)
            SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    return authorities.stream()
        .filter(authority -> authority.getAuthority().endsWith("_RESEARCHER"))
        .map(authority -> authority.getAuthority().replace("_RESEARCHER", "").replace("ROLE_", ""))
        .collect(Collectors.toSet());
  }
}
