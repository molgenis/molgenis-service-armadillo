package org.molgenis.datashield;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwtController {

  @GetMapping("/authentication")
  public Object getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
