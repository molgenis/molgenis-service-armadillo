package org.molgenis.armadillo.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
// temporary controller until we have proper UI
public class WelcomeController {
  private final String HTML =
      """
            <html>
            <head>
              <title>Armadillo</title>
              <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.4.1/dist/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
            <head>
            <body>
            <div class=\"container\">
            <h1>Welcome to Armadillo.</h1>
            %s
            <br/><br/>
            <a href="/login">Login using local account</a><br/>
            <a href="/oauth2/">Login using institute account</a><br/>
            <a href="/swagger-ui/index.html">Go to Swagger user interface</a><br/>
            <a href="/logout">Logout of local account</a>
            </div>
            </body>
            </html>
            """;

  @GetMapping("/")
  @ResponseBody
  public String indexHtml() {
    return String.format(HTML, "");
  }

  @GetMapping("/error")
  @ResponseBody
  public String errorHtml() {
    return String.format(
        HTML,
        """
            <div class="alert alert-primary" role="alert">
              Something went wrong. Did admin possibly not set oath2 properly?
            </div>
            """);
  }
}
