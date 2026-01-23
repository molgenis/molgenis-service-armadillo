// package org.molgenis.armadillo.security;
//
// import static java.util.Collections.emptySet;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.Mockito.when;
//
// import java.util.Collection;
// import java.util.List;
// import java.util.Map;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.molgenis.armadillo.metadata.ArmadilloMetadata;
// import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
// <<<<<<< HEAD
// =======
// import org.molgenis.armadillo.metadata.MetadataLoader;
// import org.molgenis.armadillo.metadata.ProjectDetails;
// import org.molgenis.armadillo.metadata.ProjectPermission;
// import org.molgenis.armadillo.metadata.UserDetails;
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
// import org.molgenis.armadillo.storage.ArmadilloStorageService;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.oauth2.jwt.Jwt;
// import org.springframework.test.util.ReflectionTestUtils;
//
// @ExtendWith(MockitoExtension.class)
// class JwtRolesExtractorTest {
//  @Mock Jwt jwt;
//  @Mock ArmadilloStorageService armadilloStorage;
// <<<<<<< HEAD
// =======
//  @Mock MetadataLoader metadataLoader;
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//  ArmadilloMetadataService armadilloMetadataService;
//
//  @Test
//  void convertTest() {
//    when(jwt.getClaims()).thenReturn(Map.of("roles", List.of("lifecycle_RESEARCHER")));
//    when(jwt.getClaimAsString("email")).thenReturn("bofke@email.com");
// <<<<<<< HEAD
//    // local only
//    when(armadilloStorage.loadSystemFile(METADATA_FILE))
//        .thenReturn(
//            new ByteArrayInputStream(
//                "{\"users\":{\"bofke@email.com\":{\"email\":\"bofke@email.com\",
//                    .getBytes()));
//    armadilloMetadataService.reload();
// =======
//
//    var metadata = ArmadilloMetadata.create();
//    metadata.getUsers().put("bofke@email.com", UserDetails.createAdmin("bofke@email.com"));
//    metadata.getProjects().put("myproject", ProjectDetails.create("myproject", emptySet()));
//    metadata.getPermissions().add(ProjectPermission.create("bofke@email.com", "myproject"));
//    when(metadataLoader.load()).thenReturn(metadata);
//
//    armadilloMetadataService = new ArmadilloMetadataService(armadilloStorage, metadataLoader,
// null);
// >>>>>>> 7116458d5c90bb7d947200a3b014e573be12bc7d
//    Collection<GrantedAuthority> authorities =
//        new JwtRolesExtractor(armadilloMetadataService).convert(jwt);
//    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MYPROJECT_RESEARCHER")));
//    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SU")));
//    // when role giving from central oauth is disable this shouldn't happen
//    assertFalse(authorities.contains(new SimpleGrantedAuthority("ROLE_LIFECYCLE_RESEARCHER")));
//
//    // with option of oidc role extraction
//    ReflectionTestUtils.setField(armadilloMetadataService, "oidcPermissionsEnabled", true);
//
//    authorities = new JwtRolesExtractor(armadilloMetadataService).convert(jwt);
//    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_LIFECYCLE_RESEARCHER")));
//    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MYPROJECT_RESEARCHER")));
//    assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_SU")));
//
//    System.clearProperty("datashield.oidc-permission-enabled");
//  }
// }
