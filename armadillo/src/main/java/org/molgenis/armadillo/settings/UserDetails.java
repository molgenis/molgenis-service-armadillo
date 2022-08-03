package org.molgenis.armadillo.settings;

import com.google.auto.value.AutoValue;
import java.util.HashSet;
import java.util.Set;

@AutoValue
public abstract class UserDetails {
  public abstract String getFirstName();

  public abstract String getLastName();

  public abstract String getInstitution();

  public abstract Set<String> getProjects();

  public static UserDetails create(
      String firstName, String lastName, String institution, Set<String> projects) {
    return new AutoValue_UserDetails(firstName, lastName, institution, projects);
  }

  public static UserDetails create() {
    return new AutoValue_UserDetails(null, null, null, new HashSet<>());
  }
}
