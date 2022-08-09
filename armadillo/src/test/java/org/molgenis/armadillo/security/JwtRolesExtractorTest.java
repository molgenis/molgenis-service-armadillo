package org.molgenis.armadillo.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.metadata.ArmadilloMetadataService.METADATA_FILE;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
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
    ArmadilloMetadataService armadilloMetadataService =
        new ArmadilloMetadataService(armadilloStorage);

    when(armadilloStorage.loadSystemFile(METADATA_FILE))
        .thenReturn(
            new ByteArrayInputStream(
                "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\", \"admin\":true}},\"projects\":{\"myproject\":{\"name\":\"myproject\"}},\"permissions\":[{\"email\":\"bofke@email.com\",\"project\":\"myproject\"}]}"
                    .getBytes()));
    armadilloMetadataService.reload();

    when(jwt.getClaimAsString("email")).thenReturn("bofke@email.com");

    Collection<GrantedAuthority> authorities =
        new JwtRolesExtractor(armadilloMetadataService).convert(jwt);
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_myproject_RESEARCHER")));
    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SU")));
  }
}
