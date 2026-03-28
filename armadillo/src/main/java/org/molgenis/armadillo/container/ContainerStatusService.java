package org.molgenis.armadillo.container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.armadillo.metadata.ContainerStartStatus;
import org.springframework.stereotype.Service;

@Service
public class ContainerStatusService {
  private final Map<String, ContainerStartStatus> statuses = new ConcurrentHashMap<>();

  public void updateStatus(
      String containerName, String status, Integer completedLayers, Integer totalLayers) {
    statuses.put(
        containerName,
        new ContainerStartStatus(containerName, status, completedLayers, totalLayers));
  }

  public ContainerStartStatus getStatus(String containerName) {
    return statuses.getOrDefault(containerName, new ContainerStartStatus(null, null, null, null));
  }
}
