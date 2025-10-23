package org.molgenis.armadillo.metadata;

public class ProfileStartStatus {
  private String state; // "PULLING", "STARTING", "UNKNOWN"
  private int percent; // 0–100
  private Integer completedLayers; // null when not applicable
  private Integer totalLayers; // null when not applicable

  public ProfileStartStatus(
      String state, int percent, Integer completedLayers, Integer totalLayers) {
    this.state = state;
    this.percent = percent;
    this.completedLayers = completedLayers;
    this.totalLayers = totalLayers;
  }

  public String getState() {
    return state;
  }

  public int getPercent() {
    return percent;
  }

  public Integer getCompletedLayers() {
    return completedLayers;
  }

  public Integer getTotalLayers() {
    return totalLayers;
  }

  public void setState(String state) {
    this.state = state;
  }

  public void setPercent(int percent) {
    this.percent = percent;
  }

  public void setCompletedLayers(Integer completedLayers) {
    this.completedLayers = completedLayers;
  }

  public void setTotalLayers(Integer totalLayers) {
    this.totalLayers = totalLayers;
  }
}
