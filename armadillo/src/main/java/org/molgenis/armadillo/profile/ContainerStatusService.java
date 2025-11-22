package org.molgenis.armadillo.profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.armadillo.metadata.ContainerStartStatus;
import org.springframework.stereotype.Service;

@Service
public class ContainerStatusService {
  private final Map<String, ContainerStartStatus> statuses = new ConcurrentHashMap<>();

  public void updateStatus(
      String profileName, String status, Integer completedLayers, Integer totalLayers) {
    statuses.put(
        profileName, new ContainerStartStatus(profileName, status, completedLayers, totalLayers));
  }

  public ContainerStartStatus getStatus(String profileName) {
    return statuses.getOrDefault(profileName, new ContainerStartStatus(null, null, null, null));
  }
}
