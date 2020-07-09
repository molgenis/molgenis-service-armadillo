package org.molgenis.armadillo.info;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;

@ExtendWith(MockitoExtension.class)
class AuthInfoContributorTest {

  @Mock OAuth2ResourceServerProperties properties;
  @Mock OAuth2ResourceServerProperties.Jwt jwt;
  @Mock OAuth2ResourceServerProperties.Opaquetoken opaquetoken;
  @Mock Info.Builder builder;

  @Test
  void testContribute() {
    when(properties.getJwt()).thenReturn(jwt);
    when(jwt.getIssuerUri()).thenReturn("https://auth.example.org/");
    when(properties.getOpaquetoken()).thenReturn(opaquetoken);
    when(opaquetoken.getClientId()).thenReturn("407e5ebc-c105-11ea-b3de-0242ac130004");
    AuthInfoContributor contributor = new AuthInfoContributor(properties);

    contributor.contribute(builder);

    verify(builder)
        .withDetail(
            "auth",
            Map.of(
                "issuerUri",
                "https://auth.example.org/",
                "clientId",
                "407e5ebc-c105-11ea-b3de-0242ac130004"));
  }
}
