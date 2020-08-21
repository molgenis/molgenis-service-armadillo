package org.molgenis.armadillo;

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
public class ArmadilloPermissionEvaluator implements PermissionEvaluator {

  public static final String PROJECT = "Project";
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
    if (!PROJECT.equals(targetType)) {
      throw new IllegalArgumentException("Can only check project permissions");
    }
    return hasProjectPermission(authentication, (String) targetId, (String) permission);
  }

  private static Set<String> getRoles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .collect(toUnmodifiableSet());
  }

  boolean hasProjectPermission(
      Authentication authentication, String projectName, String permission) {
    if (!LOAD.equals(permission)) {
      throw new IllegalArgumentException("Can only check load permission");
    }
    String roleName = format("ROLE_%s_RESEARCHER", projectName.toUpperCase());
    return getRoles(authentication).contains(roleName);
  }
}
