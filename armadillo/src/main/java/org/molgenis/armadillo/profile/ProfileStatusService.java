package org.molgenis.armadillo.profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.armadillo.metadata.ProfileStartStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileStatusService {

  private final Map<String, ProfileStartStatus> statuses = new ConcurrentHashMap<>();

  // New: full update (percent + layers)
  public void updateStatus(
      String profileName,
      String state,
      int percent,
      Integer completedLayers,
      Integer totalLayers,
      String layerStatus,
      Integer layerPercent) {
    statuses.put(
        profileName,
        new ProfileStartStatus(
            state, percent, completedLayers, totalLayers, layerStatus, layerPercent));
  }

  // Back-compat helper if you ever call without layer counts
  public void updateStatus(String profileName, String state, int percent) {
    updateStatus(profileName, state, percent, null, null, null, null);
  }

  public ProfileStartStatus getStatus(String profileName) {
    return statuses.getOrDefault(
        profileName, new ProfileStartStatus("UNKNOWN", 0, null, null, null, null));
  }
}
