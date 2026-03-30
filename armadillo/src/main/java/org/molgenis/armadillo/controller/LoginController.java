package org.molgenis.armadillo.controller;

import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
@Hidden
public class LoginController {
  private final ManagementService managementService;
  SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

  public LoginController(ManagementService managementService) {
    this.managementService = requireNonNull(managementService);
  }

  @GetMapping("/oauth2/")
  public RedirectView whenAuthenticatedRedirect() {
    return new RedirectView("/");
  }

  @GetMapping("/oauth2-logout")
  public RedirectView oAuthLogout() {
    return new RedirectView("/");
  }

  @GetMapping("/basic-login")
  public RedirectView basicLogin(Principal principal) {
    return new RedirectView("/");
  }

  @GetMapping("/basic-logout")
  public void basicLogout(HttpServletResponse response) {
    response.setStatus(401);
    response.addHeader("WWW-Authenticate", "Basic realm=\"Armadillo\"");
  }

  @Bean
  public HttpSessionEventPublisher sessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  JsonObject doGetRequest(String uri) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).GET().build();
    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return new Gson().fromJson(response.body(), JsonObject.class);
    } catch (IOException | InterruptedException e) {
      throw new UnsupportedOperationException("Cannot retrieve " + uri);
    }
  }

  JsonElement getLogoutUri() {
    String oauthServer =
        managementService.getOidcConfig().issuerUri() + "/.well-known/openid-configuration";
    try {
      JsonObject responseBody = doGetRequest(oauthServer);
      return responseBody.get("end_session_endpoint");
    } catch (UnsupportedOperationException e) {
      throw new UnsupportedOperationException(
          "Cannot retrieve auth server logout URL; logging out not possible via armadillo");
    }
  }

  @GetMapping("/oauth-logout")
  public void oAuthLogout(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    // 1. Fetch logout URI BEFORE invalidating the session
    String logoutUri = getLogoutUri().getAsString();

    // 2. Get id_token_hint before clearing security context
    String idTokenHint = null;
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof OidcUser oidcUser) {
      idTokenHint = oidcUser.getIdToken().getTokenValue();
    }

    // 3. NOW invalidate the local session
    logoutHandler.logout(request, response, auth);

    // 4. Build the redirect URL
    // test
    //    String postLogoutRedirectUri = request.getRequestURL().toString() + "/login";
    // replace with the oauht redirect login uri?
    String postLogoutRedirectUri = "http://localhost:8080/#/login";
    StringBuilder redirectUrl =
        new StringBuilder(logoutUri)
            .append("?post_logout_redirect_uri=")
            .append(URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8));

    if (idTokenHint != null) {
      redirectUrl
          .append("&id_token_hint=")
          .append(URLEncoder.encode(idTokenHint, StandardCharsets.UTF_8));
    }

    // 5. Send a 302 so the BROWSER navigates to the auth server (carries its own cookies)
    response.sendRedirect(redirectUrl.toString());
  }
}
