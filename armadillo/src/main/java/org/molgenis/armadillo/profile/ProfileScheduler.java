package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.Map;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ProfileScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileScheduler.class);

  private final DockerService dockerService;
  private final ProfileService profileService;

  public ProfileScheduler(DockerService dockerService, ProfileService profileService) {
    this.dockerService = dockerService;
    this.profileService = profileService;
  }

  @Scheduled(cron = "0 */3 * * * *")
  public void restartRunningProfiles() {
    LOG.info(
        "Scheduled task: checking for updated images and restarting running profiles if needed");

    runAsSystem(
        () -> {
          Map<String, ContainerInfo> allStatuses = dockerService.getAllProfileStatuses();

          allStatuses.forEach(
              (profileName, containerInfo) -> {
                if (containerInfo.getStatus() == ProfileStatus.RUNNING) {
                  try {
                    var profileConfig = profileService.getByName(profileName);

                    if (Boolean.TRUE.equals(profileConfig.getAutoUpdate())) {
                      boolean updated = dockerService.pullImageIfUpdated(profileConfig);
                      if (updated) {
                        LOG.info("Image updated for '{}', restarting...", profileName);
                        dockerService.startProfile(profileName);
                      } else {
                        LOG.info("No image update for '{}', skipping restart", profileName);
                      }
                    } else {
                      LOG.info("Auto-update disabled for '{}', skipping", profileName);
                    }
                  } catch (Exception e) {
                    LOG.error(
                        "Error while checking profile '{}': {}", profileName, e.getMessage(), e);
                  }
                }
              });

          return null;
        });
  }
}
