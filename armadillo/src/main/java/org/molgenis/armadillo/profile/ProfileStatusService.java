package org.molgenis.armadillo.profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.armadillo.metadata.ProfileStartStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileStatusService {
  private final Map<String, ProfileStartStatus> statuses = new ConcurrentHashMap<>();

  public void updateStatus(
      String profileName, String status, Integer completedLayers, Integer totalLayers) {
    statuses.put(
        profileName, new ProfileStartStatus(profileName, status, completedLayers, totalLayers));
  }

  public ProfileStartStatus getStatus(String profileName) {
    return statuses.getOrDefault(profileName, new ProfileStartStatus(null, null, null, null));
  }
}
