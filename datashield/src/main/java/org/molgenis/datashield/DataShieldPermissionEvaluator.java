package org.molgenis.datashield;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class DataShieldPermissionEvaluator implements PermissionEvaluator {

  public static final String WORKSPACE = "Workspace";
  public static final String ROLE_SU = "ROLE_SU";
  public static final String LOAD = "load";

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    if (getRoles(authentication).contains(ROLE_SU)) {
      return true;
    }
    if (targetId instanceof Collection) {
      Collection<String> objects = (Collection<String>) targetId;
      return objects.stream()
          .allMatch(it -> hasPermission(authentication, it, targetType, permission));
    }
    switch (targetType) {
      case WORKSPACE:
        return hasWorkspacePermission(authentication, (String) targetId, (String) permission);
      default:
        throw new UnsupportedOperationException("Can only check workspace permissions");
    }
  }

  private static Set<String> getRoles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(toUnmodifiableSet());
  }

  boolean hasWorkspacePermission(
      Authentication authentication, String objectName, String permission) {
    switch (permission) {
      case LOAD:
        {
          String folder = objectName.substring(0, objectName.indexOf('/'));
          return getRoles(authentication).contains(format("ROLE_%s_RESEARCHER", folder));
        }
      default:
        throw new IllegalArgumentException("Can only check workspace load permission");
    }
  }
}
