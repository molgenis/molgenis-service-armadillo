package org.molgenis.armadillo.metadata;

public class ContainerStartStatus {
  private String containerName;
  private String status;
  private Integer completedLayers;
  private Integer totalLayers;

  public ContainerStartStatus(
      String containerName, String status, Integer completedLayers, Integer totalLayers) {
    this.containerName = containerName;
    this.status = status;
    this.completedLayers = completedLayers;
    this.totalLayers = totalLayers;
  }

  public String getContainerName() {
    return containerName;
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

  public void setContainerName(String containerName) {
    this.containerName = containerName;
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
