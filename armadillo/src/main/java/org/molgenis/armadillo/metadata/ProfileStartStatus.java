package org.molgenis.armadillo.metadata;

public class ProfileStartStatus {
  private String profileName;
  private String status;
  private Integer completedLayers;
  private Integer totalLayers;

  public ProfileStartStatus(
      String profileName, String status, Integer completedLayers, Integer totalLayers) {
    this.profileName = profileName;
    this.status = status;
    this.completedLayers = completedLayers;
    this.totalLayers = totalLayers;
  }

  public String getProfileName() {
    return profileName;
  }

  public String getStatus() {
    return status;
  }

  public Integer getCompletedLayers() {
    return completedLayers;
  }

  public Integer getTotalLayers() {
    return totalLayers;
  }

  public void setProfileName(String profileName) {
    this.profileName = profileName;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setCompletedLayers(Integer completedLayers) {
    this.completedLayers = completedLayers;
  }

  public void setTotalLayers(Integer totalLayers) {
    this.totalLayers = totalLayers;
  }
}
