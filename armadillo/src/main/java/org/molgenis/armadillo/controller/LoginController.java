package org.molgenis.armadillo.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
@Hidden
public class LoginController {
  @GetMapping("/oauth2/")
  public RedirectView whenAuthenticatedRedirect() {
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
}
