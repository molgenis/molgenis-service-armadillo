// package org.molgenis.armadillo.security;
//
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
//
// import java.security.KeyPair;
// import java.security.KeyPairGenerator;
// import java.security.interfaces.RSAPublicKey;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import
// org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
// import org.springframework.security.oauth2.jwt.*;
//
// @ExtendWith(MockitoExtension.class)
// class JwtDecoderConfigTest {
//
//  @Mock private OAuth2ResourceServerProperties properties;
//
//  @Mock private OAuth2ResourceServerProperties.Jwt jwtProps;
//
//  @Mock private OAuth2ResourceServerProperties.Opaquetoken opaqueProps;
//
//  @Mock private ResourceTokenService resourceTokenService;
//
//  private JwtDecoderConfig config;
//
//  @BeforeEach
//  void setup() {
//    config = new JwtDecoderConfig();
//  }
//
//  @Test
//  void offlineProfile_returnsFailingDecoderWhenConfigurationFails() {
//    // Arrange
//    setActiveProfile("offline");
//
//    when(properties.getJwt()).thenThrow(new RuntimeException("boom"));
//
//    // Act
//    JwtDecoder decoder = config.jwtDecoder(properties, resourceTokenService);
//
//    // Assert
//    UnsupportedOperationException ex =
//        assertThrows(UnsupportedOperationException.class, () -> decoder.decode("token"));
//
//    assertTrue(ex.getMessage().contains("JWT configuration failed"));
//  }
//
//  @Test
//  void nonOfflineProfile_rethrowsConfigurationException() {
//    // Arrange
//    setActiveProfile("prod");
//    when(properties.getJwt()).thenThrow(new RuntimeException("boom"));
//
//    // Act + Assert
//    assertThrows(RuntimeException.class, () -> config.jwtDecoder(properties,
// resourceTokenService));
//  }
//
//  // ---------- helpers ----------
//
//  private void setActiveProfile(String profile) {
//    try {
//      var field = JwtDecoderConfig.class.getDeclaredField("activeProfile");
//      field.setAccessible(true);
//      field.set(config, profile);
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  private RSAPublicKey generatePublicKey() throws Exception {
//    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
//    gen.initialize(2048);
//    KeyPair pair = gen.generateKeyPair();
//    return (RSAPublicKey) pair.getPublic();
//  }
// }

package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

class JwtDecoderConfigTest {

  private JwtDecoderConfig jwtDecoderConfig;
  private NimbusJwtDecoder mockInternalDecoder;
  private Jwt validJwtToken;
  private final String issuerUri = "http://example.com";
  private final String validToken = "valid.jwt.token";
  private final String invalidToken = "invalid.jwt.token";

  @Mock private OAuth2ResourceServerProperties properties;

  @Mock private ResourceTokenService resourceTokenService;

  @Mock private NimbusJwtDecoder externalDecoder;

  @Mock private NimbusJwtDecoder internalDecoder;

  @Mock private Logger logger;

  @Value("${spring.profiles.active:default}")
  private String activeProfile;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    jwtDecoderConfig = new JwtDecoderConfig();
    // Inject mocks
    JwtDecoderConfig.LOG = logger;
    validJwtToken = mock(Jwt.class);
    // Mock the external decoder setup
    when(properties.getJwt()).thenReturn(mock(OAuth2ResourceServerProperties.Jwt.class));
    when(properties.getJwt().getIssuerUri()).thenReturn(issuerUri);
    // Mock ResourceTokenService
    RSAPublicKey mockPublicKey = mock(RSAPublicKey.class);
    when(resourceTokenService.getPublicKey()).thenReturn(mockPublicKey);
    // Mock the Opaquetoken and ClientId
    OAuth2ResourceServerProperties.Opaquetoken mockOpaquetoken =
        mock(OAuth2ResourceServerProperties.Opaquetoken.class);
    when(mockOpaquetoken.getClientId()).thenReturn("test-client-id");
    // Mock the OAuth2ResourceServerProperties to return the mocked Opaquetoken
    when(properties.getOpaquetoken()).thenReturn(mockOpaquetoken);
    // Mock the NimbusJwtDecoder for internal decoder (simulate failure)
    mockInternalDecoder = mock(NimbusJwtDecoder.class);
  }

  //  @Test
  //  void testGetAudienceValidator() throws JwtException {
  //    String validTokenWithMatchingAudience = "valid.jwt.token";
  //    String validTokenWithNonMatchingAudience = "invalid.jwt.token";
  //
  //    // Mock the JWT token and its claims
  //    Jwt invalidJwtToken = mock(Jwt.class);
  //
  //    when(validJwtToken.getClaim(AUD)).thenReturn(Arrays.asList("test-client-id"));
  //    when(invalidJwtToken.getClaim(AUD)).thenReturn(Arrays.asList("invalid-client-id"));
  //
  //    // Mock the properties and the clientId
  //    when(properties.getOpaquetoken().getClientId()).thenReturn("test-client-id");
  //
  //    // Mock external and internal decoders to simulate the validation flow
  //    when(externalDecoder.decode(validTokenWithMatchingAudience)).thenReturn(validJwtToken);
  //    when(externalDecoder.decode(validTokenWithNonMatchingAudience)).thenReturn(invalidJwtToken);
  //
  //    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
  //      mockedStatic.when(() ->
  // JwtDecoders.fromIssuerLocation(issuerUri)).thenReturn(mockInternalDecoder);
  //      // Create a JWT decoder from the configuration
  //      JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);
  //
  //      // Test valid token with matching audience
  //      Jwt decodedToken = jwtDecoder.decode(validTokenWithMatchingAudience);
  //      assertNotNull(decodedToken);
  //      verify(externalDecoder, times(1)).decode(validTokenWithMatchingAudience);
  //
  //      // Test valid token with non-matching audience
  //      Jwt decodedInvalidToken = jwtDecoder.decode(validTokenWithNonMatchingAudience);
  //      assertNull(decodedInvalidToken);  // Should throw an exception or return null depending on
  // the implementation
  //      verify(externalDecoder, times(1)).decode(validTokenWithNonMatchingAudience);
  //    }
  //  }

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
    String validTokenWithMatchingAudience = "valid.jwt.token";

    // Mock the NimbusJwtDecoder to return a valid Jwt when decoding
    when(mockInternalDecoder.decode(validTokenWithMatchingAudience)).thenReturn(validJwtToken);

    // Mock the static method JwtDecoders.fromIssuerLocation() to return the mocked NimbusJwtDecoder
    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
      mockedStatic
          .when(() -> JwtDecoders.fromIssuerLocation(issuerUri))
          .thenReturn(mockInternalDecoder);

      // Now, create the JWT decoder using the configuration
      JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);

      // Decode the token
      Jwt decodedToken = jwtDecoder.decode(validTokenWithMatchingAudience);

      // Assert the results
      assertNotNull(decodedToken);
      verify(mockInternalDecoder, times(1)).decode(validTokenWithMatchingAudience);
    }
  }

  @Test
  void testJwtDecoder_withInvalidToken_fallsBackToExternalDecoder() {
    String invalidToken = "invalid.jwt.token";

    when(mockInternalDecoder.decode(invalidToken))
        .thenThrow(JwtException.class); // Simulate decoding failure

    // Mock the NimbusJwtDecoder for external decoder (simulate success)
    NimbusJwtDecoder mockExternalDecoder = mock(NimbusJwtDecoder.class);
    when(mockExternalDecoder.decode(invalidToken))
        .thenReturn(validJwtToken); // Simulate valid token decoding

    // Mock the static method JwtDecoders.fromIssuerLocation() to return the mocked external decoder
    try (MockedStatic<JwtDecoders> mockedStatic = Mockito.mockStatic(JwtDecoders.class)) {
      // Mock the external decoder for the given issuer URI
      mockedStatic
          .when(() -> JwtDecoders.fromIssuerLocation(issuerUri))
          .thenReturn(mockExternalDecoder);

      // Mock the creation of jwtDecoder bean (using the configuration)
      JwtDecoder jwtDecoder = jwtDecoderConfig.jwtDecoder(properties, resourceTokenService);

      // Decode the token (this should invoke internalDecoder first and fallback to externalDecoder)
      Jwt decodedToken = jwtDecoder.decode(invalidToken);

      // Assert that the fallback mechanism was triggered (i.e., external decoder was used)
      assertNotNull(decodedToken); // Token should still be decoded
      verify(mockExternalDecoder, times(1))
          .decode(invalidToken); // Ensure external decoder was used
    }
  }

  @Test
  void testJwtDecoder_configurationThrowsException_inOfflineProfile() {
    String invalidToken = "invalid.jwt.token";
    // Simulating 'offline' profile
    jwtDecoderConfig.activeProfile = "offline";
    when(externalDecoder.decode(invalidToken)).thenThrow(JwtException.class);

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
