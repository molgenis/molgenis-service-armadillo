package org.molgenis.armadillo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

public class NoPopupBasicAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Override
  public void afterPropertiesSet() {
    setRealmName("Armadillo");
    super.afterPropertiesSet();
  }
}
