package org.molgenis.armadillo.security;

import static java.util.stream.Collectors.toSet;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Collection;
import org.molgenis.armadillo.exceptions.ForbiddenProfileException;
import org.molgenis.armadillo.exceptions.WrongProfileException;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.ProjectDetails;
import org.molgenis.armadillo.profile.ActiveProfileNameAccessor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ProfileSecurity {

  private final AccessService accessService;

  public ProfileSecurity(AccessService accessService) {
    this.accessService = accessService;
  }

  public boolean canLoadToProfile(String project) {
    var profile = ActiveProfileNameAccessor.getActiveProfileName();
    var allowedProfiles = getProjectDetails(project).getProfiles();
    if (!allowedProfiles.contains(profile)) {
      throw new WrongProfileException(project, profile, allowedProfiles);
    }
    return true;
  }

  public boolean canSelectProfile(String profile) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    var userRoles =
        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(toSet());

    if (userRoles.contains("ROLE_SU")) {
      return true;
    }

    if (userRoles.contains("ROLE_ANONYMOUS")) {
      return false;
    }

    var allowedProfiles =
        userRoles.stream()
            .map(this::roleToProject)
            .map(this::getProjectDetails)
            .map(ProjectDetails::getProfiles)
            .flatMap(Collection::stream)
            .collect(toSet());

    if (!allowedProfiles.contains(profile)) {
      throw new ForbiddenProfileException(profile, allowedProfiles);
    }

    return true;
  }

  private ProjectDetails getProjectDetails(String project) {
    return runAsSystem(() -> accessService.projectsByName(project));
  }

  private String roleToProject(String role) {
    return role.replace("_RESEARCHER", "").replace("ROLE_", "").toLowerCase();
  }
}
