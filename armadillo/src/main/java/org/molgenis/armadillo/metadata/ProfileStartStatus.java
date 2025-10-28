package org.molgenis.armadillo.metadata;

public class ProfileStartStatus {
  private String globalStatus;
  private int totalPercent;
  private Integer completedLayers; // null when not applicable
  private Integer totalLayers; // null when not applicable
  private String layerStatus;
  private Integer layerPercent;

  public ProfileStartStatus(
      String globalStatus,
      int totalPercent,
      Integer completedLayers,
      Integer totalLayers,
      String layerStatus,
      Integer layerPercent) {
    this.globalStatus = globalStatus;
    this.totalPercent = totalPercent;
    this.completedLayers = completedLayers;
    this.totalLayers = totalLayers;
    this.layerStatus = layerStatus;
    this.layerPercent = layerPercent;
  }

  public String getGlobalStatus() {
    return globalStatus;
  }

  public int getPercent() {
    return totalPercent;
  }

  public Integer getCompletedLayers() {
    return completedLayers;
  }

  public Integer getTotalLayers() {
    return totalLayers;
  }

  public String getLayerStatus() {
    return layerStatus;
  }

  public Integer getLayerPercent() {
    return layerPercent;
  }

  public void setState(String globalStatus) {
    this.globalStatus = globalStatus;
  }

  public void setPercent(int percent) {
    this.totalPercent = percent;
  }

  public void setCompletedLayers(Integer completedLayers) {
    this.completedLayers = completedLayers;
  }

  public void setTotalLayers(Integer totalLayers) {
    this.totalLayers = totalLayers;
  }

  public void setLayerStatus(String layerStatus) {
    this.layerStatus = layerStatus;
  }

  public void setLayerPercent(Integer layerPercent) {
    this.layerPercent = layerPercent;
  }
}
