package org.molgenis.armadillo.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.audit.AuditEventPublisher.ANONYMOUS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.getUser;

import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

class AuditEventPublisherTest {

  @Test
  void testGetAnonymousUser() {
    assertEquals(ANONYMOUS, getUser(null));
  }

  @Test
  void testGetOidcUser() {
    var principal = mock(OAuth2AuthenticationToken.class, RETURNS_DEEP_STUBS);
    when(principal.getPrincipal().getAttribute("email")).thenReturn("henk@molgenis.nl");

    assertEquals("henk@molgenis.nl", getUser(principal));
  }

  @Test
  void testGetBasicAuthUser() {
    var principal = mock(Principal.class);
    when(principal.getName()).thenReturn("admin");

    assertEquals("admin", getUser(principal));
  }
}
