package org.molgenis.armadillo.security;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import org.molgenis.armadillo.exceptions.ProfileNotAllowedException;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.profile.ActiveProfileNameAccessor;
import org.springframework.stereotype.Component;

@Component
public class ProfileSecurity {
  private final AccessService accessService;

  public ProfileSecurity(AccessService accessService) {
    this.accessService = accessService;
  }

  public boolean canLoadToProfile(String project) {
    var profile = ActiveProfileNameAccessor.getActiveProfileName();
    var allowedProfiles = runAsSystem(() -> accessService.projectsByName(project).getProfiles());
    if (!allowedProfiles.contains(profile)) {
      // throw exception instead of returning false, so we get a 403 with a friendly message
      throw new ProfileNotAllowedException(project, profile, allowedProfiles);
    }
    return true;
  }
}
