package org.molgenis.armadillo.settings;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// for generation to JSON
public class User {
  private String firstName;
  private String lastName;
  private String institution;
  private Set<String> projects = new HashSet<>();

  public User(String firstName, String lastName, String institution, Set<String> projects) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.institution = institution;
    this.projects = projects;
  }

  public User() {}

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  public Set<String> getProjects() {
    return projects;
  }

  public void setProjects(Set<String> projects) {
    Objects.requireNonNull(projects);
    this.projects = projects;
  }
}
