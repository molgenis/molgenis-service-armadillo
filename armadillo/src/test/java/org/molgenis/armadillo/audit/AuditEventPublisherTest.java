package org.molgenis.armadillo.audit;

class AuditEventPublisherTest {

  //  @Test
  //  void testGetAnonymousUser() {
  //    assertEquals(ANONYMOUS, getUser(null));
  //  }

  //  @Test
  //  void testGetOidcUser() {
  //    var principal = mock(OAuth2AuthenticationToken.class, RETURNS_DEEP_STUBS);
  //    when(principal.getPrincipal().getAttribute("email")).thenReturn("henk@molgenis.nl");
  //
  //    assertEquals("henk@molgenis.nl", getUser(principal));
  //  }

  //  @Test
  //  void testGetBasicAuthUser() {
  //    var principal = mock(Principal.class);
  //    when(principal.getName()).thenReturn("admin");
  //
  //    assertEquals("admin", getUser(principal));
  //  }
  //
  //  @Test
  //  void testJwtToken() {
  //    var principal = mock(JwtAuthenticationToken.class, RETURNS_DEEP_STUBS);
  //    when(principal.getTokenAttributes().get("email")).thenReturn("tommy@molgenis.nl");
  //
  //    assertEquals("tommy@molgenis.nl", getUser(principal));
  //  }
  //
  //  @Test
  //  void testJwt() {
  //    var principal = mock(Jwt.class, RETURNS_DEEP_STUBS);
  //    when(principal.getClaims().get("email")).thenReturn("tommy@molgenis.nl");
  //    assertEquals("tommy@molgenis.nl", getUser(principal));
  //  }
  //
  //  @Test
  //  void testAuthenticationOfUser() {
  //    var principal = mock(DefaultOAuth2User.class, RETURNS_DEEP_STUBS);
  //    when(principal.getAttributes().get("email")).thenReturn("bofke@molgenis.nl");
  //
  //    assertEquals("bofke@molgenis.nl", getUser(principal));
  //  }
  //
  //  @Test
  //  void testLoginBasicAuthUser() {
  //    var principal = mock(User.class);
  //    when(principal.getUsername()).thenReturn("admin");
  //
  //    assertEquals("admin", getUser(principal));
  //  }
}
