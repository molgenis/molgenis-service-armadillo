package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.security.oauth2.jwt.*;

@ExtendWith(MockitoExtension.class)
class JwtDecoderConfigTest {

  @Mock private OAuth2ResourceServerProperties properties;

  @Mock private OAuth2ResourceServerProperties.Jwt jwtProps;

  @Mock private OAuth2ResourceServerProperties.Opaquetoken opaqueProps;

  @Mock private ResourceTokenService resourceTokenService;

  private JwtDecoderConfig config;

  @BeforeEach
  void setup() {
    config = new JwtDecoderConfig();
  }

  @Test
  void offlineProfile_returnsFailingDecoderWhenConfigurationFails() {
    // Arrange
    setActiveProfile("offline");

    when(properties.getJwt()).thenThrow(new RuntimeException("boom"));

    // Act
    JwtDecoder decoder = config.jwtDecoder(properties, resourceTokenService);

    // Assert
    UnsupportedOperationException ex =
        assertThrows(UnsupportedOperationException.class, () -> decoder.decode("token"));

    assertTrue(ex.getMessage().contains("JWT configuration failed"));
  }

  @Test
  void nonOfflineProfile_rethrowsConfigurationException() {
    // Arrange
    setActiveProfile("prod");
    when(properties.getJwt()).thenThrow(new RuntimeException("boom"));

    // Act + Assert
    assertThrows(RuntimeException.class, () -> config.jwtDecoder(properties, resourceTokenService));
  }

  // ---------- helpers ----------

  private void setActiveProfile(String profile) {
    try {
      var field = JwtDecoderConfig.class.getDeclaredField("activeProfile");
      field.setAccessible(true);
      field.set(config, profile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private RSAPublicKey generatePublicKey() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    KeyPair pair = gen.generateKeyPair();
    return (RSAPublicKey) pair.getPublic();
  }
}
