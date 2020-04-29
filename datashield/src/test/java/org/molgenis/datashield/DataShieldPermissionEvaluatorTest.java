package org.molgenis.datashield;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class DataShieldPermissionEvaluatorTest {
  private DataShieldPermissionEvaluator permissionEvaluator = new DataShieldPermissionEvaluator();
  @Mock Authentication authentication;

  @Test
  public void testRoleSUCanLoadAnything() {
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_SU"))).when(authentication).getAuthorities();
    assertTrue(
        permissionEvaluator.hasPermission(authentication, "GECKO/patient", "Workspace", "load"));
  }

  @Test
  public void testResearcherCanLoadMatchingWorkspaces() {
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_GECKO_RESEARCHER")))
        .when(authentication)
        .getAuthorities();
    assertTrue(
        permissionEvaluator.hasPermission(authentication, "GECKO/patient", "Workspace", "load"));
  }

  @Test
  public void testCannotLoadWorkspacesWithoutMatchingRoles() {
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_OTHER_RESEARCHER")))
        .when(authentication)
        .getAuthorities();
    assertFalse(
        permissionEvaluator.hasPermission(authentication, "GECKO/patient", "Workspace", "load"));
  }

  @Test
  public void testMultipleRolesMultipleWorkspaces() {
    doReturn(
            List.of(
                new SimpleGrantedAuthority("ROLE_GECKO_RESEARCHER"),
                new SimpleGrantedAuthority("ROLE_DIABETES_RESEARCHER")))
        .when(authentication)
        .getAuthorities();
    assertTrue(
        permissionEvaluator.hasPermission(
            authentication,
            newArrayList("GECKO/patient", "DIABETES/patient"),
            "Workspace",
            "load"));
  }

  @Test
  public void testMissingRoleMultipleWorkspaces() {
    doReturn(List.of(new SimpleGrantedAuthority("ROLE_GECKO_RESEARCHER")))
        .when(authentication)
        .getAuthorities();
    assertFalse(
        permissionEvaluator.hasPermission(
            authentication,
            newArrayList("GECKO/patient", "DIABETES/patient"),
            "Workspace",
            "load"));
  }
}
