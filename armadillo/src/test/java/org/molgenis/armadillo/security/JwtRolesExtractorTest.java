package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.controller.SettingsControllerTest.EXAMPLE_SETTINGS;
import static org.molgenis.armadillo.settings.ArmadilloSettingsService.SETTINGS_FILE;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.settings.ArmadilloSettingsService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
public class JwtRolesExtractorTest {
  @Mock Jwt jwt;
  @Mock ArmadilloStorageService armadilloStorage;

  @Test
  public void convertTest() {
    when(armadilloStorage.loadSystemFile(SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(EXAMPLE_SETTINGS.getBytes()));

    ArmadilloSettingsService armadilloSettingsService =
        new ArmadilloSettingsService(armadilloStorage);

    when(jwt.getClaimAsString("email")).thenReturn("bofke@email.com");

    Collection<GrantedAuthority> authorities =
        new JwtRolesExtractor(armadilloSettingsService).convert(jwt);
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_myproject_RESEARCHER")));
  }
}
