package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

class JwtDecoderConfigTest {
  private JwtDecoderConfig jwtDecoderConfig;
  private NimbusJwtDecoder mockInternalDecoder;
  private NimbusJwtDecoder mockExternalDecoder;
  private Jwt validJwtToken;
  private final String issuerUri = "http://example.com";
  private final String invalidToken = "invalid.jwt.token";

  @Mock private OAuth2ResourceServerProperties properties;
  @Mock private ResourceTokenService resourceTokenService;
  @Mock private Logger logger;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    jwtDecoderConfig = new JwtDecoderConfig();
    JwtDecoderConfig.LOG = logger;
    validJwtToken = mock(Jwt.class);
    when(properties.getJwt()).thenReturn(mock(OAuth2ResourceServerProperties.Jwt.class));
    when(properties.getJwt().getIssuerUri()).thenReturn(issuerUri);
    RSAPublicKey mockPublicKey = mock(RSAPublicKey.class);
    when(resourceTokenService.getPublicKey()).thenReturn(mockPublicKey);
    OAuth2ResourceServerProperties.Opaquetoken mockOpaquetoken =
        mock(OAuth2ResourceServerProperties.Opaquetoken.class);
    when(mockOpaquetoken.getClientId()).thenReturn("test-client-id");
    when(properties.getOpaquetoken()).thenReturn(mockOpaquetoken);
    mockInternalDecoder = mock(NimbusJwtDecoder.class);
    mockExternalDecoder = mock(NimbusJwtDecoder.class);
  }

  @Test
  void testGetInternalDecoder() {
    NimbusJwtDecoder internalDecoder = jwtDecoderConfig.getInternalDecoder(resourceTokenService);
    assertNotNull(internalDecoder);
  }

  @Test
  void testGetExternalDecoder() {
    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
      mockedStatic
          .when(() -> JwtDecoders.fromIssuerLocation(issuerUri))
          .thenReturn(mockInternalDecoder);
      NimbusJwtDecoder externalDecoder = jwtDecoderConfig.getExternalDecoder(issuerUri, properties);
      assertNotNull(externalDecoder);
    }
  }

  @Test
  void testJwtDecoder_withValidToken() throws JwtException {
    String validToken = "valid.jwt.token";
    when(mockInternalDecoder.decode(validToken)).thenReturn(validJwtToken);
    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
      mockedStatic
          .when(() -> JwtDecoders.fromIssuerLocation(issuerUri))
          .thenReturn(mockInternalDecoder);
      JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);
      Jwt decodedToken = jwtDecoder.decode(validToken);
      assertNotNull(decodedToken);
      verify(mockInternalDecoder, times(1)).decode(validToken);
      verify(mockExternalDecoder, times(0)).decode(validToken);
    }
  }

  @Test
  void testJwtDecoder_withInvalidToken_fallsBackToExternalDecoder() {
    when(mockInternalDecoder.decode(invalidToken))
        .thenThrow(JwtException.class); // Simulate decoding failure
    when(mockExternalDecoder.decode(invalidToken))
        .thenReturn(validJwtToken); // Simulate valid token decoding
    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
      mockedStatic
          .when(() -> JwtDecoders.fromIssuerLocation(issuerUri))
          .thenReturn(mockExternalDecoder);
      JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);
      Jwt decodedToken = jwtDecoder.decode(invalidToken);
      assertNotNull(decodedToken); // Token should still be decoded
      verify(mockExternalDecoder, times(1))
          .decode(invalidToken); // Ensure external decoder was used
    }
  }

  @Test
  void testJwtDecoder_configurationThrowsException_inOfflineProfile() {
    jwtDecoderConfig.activeProfile = "offline";
    when(mockExternalDecoder.decode(invalidToken)).thenThrow(JwtException.class);
    JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);
    UnsupportedOperationException exception =
        assertThrows(UnsupportedOperationException.class, () -> jwtDecoder.decode(invalidToken));
    assertEquals(
        "JWT configuration failed, please check the logs. Probably the auth server is offline?",
        exception.getMessage());
  }

  @Test
  void testGetInternalValidator() {
    OAuth2TokenValidator<Jwt> internalValidator = jwtDecoderConfig.getInternalValidator();
    assertNotNull(internalValidator);
  }

  @Test
  void testGetJwtValidator() {
    JwtClaimValidator<Collection<String>> audienceValidator = mock(JwtClaimValidator.class);
    OAuth2TokenValidator<Jwt> jwtValidator =
        jwtDecoderConfig.getJwtValidator(issuerUri, audienceValidator);
    assertNotNull(jwtValidator);
  }
}
