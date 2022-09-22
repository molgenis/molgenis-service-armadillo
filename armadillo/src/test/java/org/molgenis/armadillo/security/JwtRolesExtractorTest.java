package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.molgenis.armadillo.profile.ArmadilloProfileService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtRolesExtractorTest {
  @Mock Jwt jwt;
  @Mock ArmadilloStorageService armadilloStorage;
  @Mock ArmadilloProfileService profileService;
  ArmadilloMetadataService armadilloMetadataService;

  @Test
  void convertTest() {
    when(jwt.getClaims()).thenReturn(Map.of("roles", List.of("lifecycle_RESEARCHER")));
    when(jwt.getClaimAsString("email")).thenReturn("bofke@email.com");
    // local only
    armadilloMetadataService = new ArmadilloMetadataService(armadilloStorage, profileService);
    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(
            new ByteArrayInputStream(
                "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\", \"admin\":true}},\"projects\":{\"myproject\":{\"name\":\"myproject\"}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"myproject\"}],\"profiles\":{}}"
                    .getBytes()));
    armadilloMetadataService.reload();
    Collection<GrantedAuthority> authorities =
        new JwtRolesExtractor(armadilloMetadataService).convert(jwt);
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MYPROJECT_RESEARCHER")));
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SU")));
    // when role giving from central oauth is disable this shouldn't happen
    assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_LIFECYCLE_RESEARCHER")));

    // with option of oidc role extraction
    ReflectionTestUtils.setField(armadilloMetadataService, "oidcPermissionsEnabled", true);

    authorities = new JwtRolesExtractor(armadilloMetadataService).convert(jwt);
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_LIFECYCLE_RESEARCHER")));
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MYPROJECT_RESEARCHER")));
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SU")));

    System.clearProperty("datashield.oidc-permission-enabled");
  }
}
