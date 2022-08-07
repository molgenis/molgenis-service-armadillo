package org.molgenis.armadillo.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
// temporary controller until we have proper UI
public class WelcomeController {

  @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String indexHtml() {
    return """
            <html>
            <head>
              <title>Armadillo</title>
              <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.4.1/dist/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
            <head>
            <body>
            <div class=\"container\">
            <h1>Welcome to Armadillo.</h1>
            <a href="/oauth2/">Login using institute account (oauth2)</a>.<br/>
            Otherwise you need provide JWT or basicAuth login will be displayed when authentication is required<br/>
            <br/>
            <a href="/swagger-ui/index.html">Go to Swagger user interface</a><br/>
            Here you can test the API<br/>
            <br/>
            <a href="/logout">Logout</a><br/>
            Sign out of oauth2 or basicAuth (whatever you have chosen to sign in).
            </div>
            </body>
            </html>
            """;
  }

  @GetMapping("/oauth2/")
  @ResponseBody
  public RedirectView whenAuthenticatedRedirect() {
    return new RedirectView("/swagger-ui/index.html");
  }
}
