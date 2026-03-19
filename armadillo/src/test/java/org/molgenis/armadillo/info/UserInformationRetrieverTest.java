package org.molgenis.armadillo.info;

import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.info.UserInformationRetriever.getUser;

import java.security.Principal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class UserInformationRetrieverTest {
  @Test
  void testGetAnonymousUser() {
    Assertions.assertEquals(UserInformationRetriever.ANONYMOUS, getUser(null));
  }

  @Test
  void testGetOidcUser() {
    var principal = mock(OAuth2AuthenticationToken.class, RETURNS_DEEP_STUBS);
    when(principal.getPrincipal().getAttribute("email")).thenReturn("henk@molgenis.nl");
    Assertions.assertEquals("henk@molgenis.nl", getUser(principal));
  }

  @Test
  void testGetBasicAuthUser() {
    var principal = mock(Principal.class);
    when(principal.getName()).thenReturn("admin");

    Assertions.assertEquals("admin", getUser(principal));
  }

  @Test
  void testGetObject() {
    var principal = mock(Object.class);
    when(principal.toString()).thenReturn("object");

    Assertions.assertEquals("object", getUser(principal));
  }

  @Test
  void testJwtToken() {
    var principal = mock(JwtAuthenticationToken.class, RETURNS_DEEP_STUBS);
    when(principal.getTokenAttributes().get("email")).thenReturn("tommy@molgenis.nl");

    Assertions.assertEquals("tommy@molgenis.nl", getUser(principal));
  }

  @Test
  void testJwt() {
    var principal = mock(Jwt.class, RETURNS_DEEP_STUBS);
    when(principal.getClaims().get("email")).thenReturn("tommy@molgenis.nl");
    Assertions.assertEquals("tommy@molgenis.nl", getUser(principal));
  }

  @Test
  void testAuthenticationOfUser() {
    var principal = mock(DefaultOAuth2User.class, RETURNS_DEEP_STUBS);
    when(principal.getAttribute("email")).thenReturn("bofke@molgenis.nl");

    Assertions.assertEquals("bofke@molgenis.nl", getUser(principal));
  }

  @Test
  void testLoginBasicAuthUser() {
    var principal = mock(User.class);
    when(principal.getUsername()).thenReturn("admin");

    Assertions.assertEquals("admin", getUser(principal));
  }
}
