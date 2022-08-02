package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.settings.ArmadilloSettingsService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
public class JwtRolesExtractorTest {
  @MockBean private ArmadilloSettingsService armadilloSettingsService;
  @MockBean Jwt jwt;

  @Test
  public void convertTest() {
    when(armadilloSettingsService.getGrantsForEmail("bofke@email.com"))
        .thenReturn(Set.of("myproject"));
    when(jwt.getClaimAsString("email")).thenReturn("bofke@email.com");

    Collection<GrantedAuthority> authorities =
        new JwtRolesExtractor(armadilloSettingsService).convert(jwt);
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_myproject_RESEARCHER")));
  }
}
