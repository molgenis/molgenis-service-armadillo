package org.molgenis.armadillo.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
@Hidden
// temporary controller until we have proper UI
public class WelcomeController {
  private String clientId;

  public WelcomeController(
      @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
          String clientId) {
    this.clientId = clientId;
  }

  // @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
  // @ResponseBody
  public String indexHtml(Principal principal) {
    // default: only basic auth has config and not signed in
    String loginAndLogout =
        """
            <a href="/basic-login/">Login using local account (basic-auth)</a>.<br/>
            Otherwise you need provide JWT when authentication is required.
            See manual on how to setup oauth2 (recommended).
            """;

    // when there is oauth set
    if (clientId != null) {
      loginAndLogout =
          """
              <a href="/oauth2/">Login using institute account (oauth2)</a>.<br/>
              <a href="/basic-login/">Login using local account (basic-auth)</a>.<br/>
              Otherwise you need provide JWT or basicAuth login will be displayed when authentication is required
              """;
    }

    // when basic auth authenticated
    if (principal instanceof UsernamePasswordAuthenticationToken) {
      loginAndLogout =
          "You have signed in using basic-auth with username: "
              + principal.getName()
              + """
              <br/>
              <script>
              function logout() {
                  var http = new XMLHttpRequest();
                  //get rid of session
                  http.open("get", "/logout", false);
                  //rand password to prevent caching
                  http.open("get", "/basic-logout", false, 'logout', (new Date()).getTime().toString());
                  http.send("");
                  // status 401 is for accessing unauthorized route
                  // will get this only if attempt to flush out correct credentials was successful
                  if (http.status === 401) {
                    alert("You're logged out now");
                    window.location.href = "/";
                  } else {
                    alert("Logout failed");
                  }
              }
              </script>
              <a onclick="logout()" href="#">logout</a>
              """;
    }

    // when oauth2 authenticated
    if (principal instanceof OAuth2AuthenticationToken oauth2token) {
      loginAndLogout =
          "You have signed in using oauth2 with email address: "
              + oauth2token.getPrincipal().getAttribute("email")
              + """
              <br/>
              <a href="/logout">Logout</a><br/>
              """;
    }

    // put login into the page
    return String.format(
        """
            <html>
            <head>
              <title>Armadillo</title>
              <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.4.1/dist/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
            <head>
            <body>
            <div class="container">
            <h1>Welcome to Armadillo.</h1>
            %s
            <br/><br/>
            <a href="/swagger-ui/index.html">Go to Swagger user interface</a><br/>
            Here you can test the API<br/>
            <a href="/ui/index.html">Go to new user interface (beta)</a><br/>
            Here you can test the API<br/>
            <br/>
            </div>
            </body>
            </html>
            """,
        loginAndLogout);
  }

  @GetMapping("/oauth2/")
  @ResponseBody
  public RedirectView whenAuthenticatedRedirect() {
    return new RedirectView("/");
  }

  @GetMapping("/basic-login")
  @ResponseBody
  public String basicLogin(Principal principal) {
    System.out.println(principal.getName());

    return "redirect:/";
  }

  @GetMapping("/basic-logout")
  public void basicLogout(HttpServletResponse response) {
    response.setStatus(401);
    response.addHeader("WWW-Authenticate", "Basic realm=\"Armadillo\"");
  }
}
