package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import com.github.dockerjava.api.DockerClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.molgenis.armadillo.controller.ProfilesDockerController;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.molgenis.armadillo.metadata.UpdateSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED)
public class ProfileScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileScheduler.class);

  private final DockerService dockerService;
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
  private ThreadPoolTaskScheduler taskScheduler;
  private final DockerClient dockerClient;

  public ProfileScheduler(DockerService dockerService, DockerClient dockerClient) {
    this.dockerService = dockerService;
    this.dockerClient = dockerClient;
  }

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    this.taskScheduler = new ThreadPoolTaskScheduler();
    this.taskScheduler.setPoolSize(10);
    this.taskScheduler.setThreadNamePrefix("ProfileUpdater-");
    this.taskScheduler.initialize();
    return this.taskScheduler;
  }

  /** Reschedule or create a scheduled update task for a profile. */
  public void reschedule(ContainerConfig profile) {
    cancel(profile.getName());

    if (Boolean.TRUE.equals(profile.getAutoUpdate()) && profile.getUpdateSchedule() != null) {
      String cron = toCron(profile.getUpdateSchedule());
      Runnable task = () -> runAsSystem(() -> runUpdateForProfile(profile));
      ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cron));
      scheduledTasks.put(profile.getName(), future);
      LOG.info("Scheduled auto-update for profile '{}' with cron '{}'", profile.getName(), cron);
    }
  }

  /** Cancel the scheduled task for a given profile name, if any. */
  public void cancel(String profileName) {
    ScheduledFuture<?> existing = scheduledTasks.remove(profileName);
    if (existing != null) {
      existing.cancel(false);
      LOG.info("Cancelled auto-update task for profile '{}'", profileName);
    }
  }

  private String toCron(UpdateSchedule schedule) {
    String time = schedule.time() != null ? schedule.time() : "01:00";
    String frequency = schedule.frequency() != null ? schedule.frequency() : "weekly";
    String day = schedule.day();

    String[] parts = time.split(":");
    String minute = parts[1];
    String hour = parts[0];

    if ("weekly".equalsIgnoreCase(frequency)) {
      int dayOfWeek = convertDayToCronNumber(day != null ? day : "Sunday");
      return String.format("0 %s %s * * %d", minute, hour, dayOfWeek);
    } else {
      return String.format("0 %s %s * * *", minute, hour); // daily or fallback
    }
  }

  private int convertDayToCronNumber(String day) {
    return switch (day.toLowerCase()) {
      case "sunday" -> 0;
      case "monday" -> 1;
      case "tuesday" -> 2;
      case "wednesday" -> 3;
      case "thursday" -> 4;
      case "friday" -> 5;
      case "saturday" -> 6;
      default -> 0;
    };
  }

  private void runUpdateForProfile(ContainerConfig profile) {
    try {
      // Get container status directly using profile name
      var containerInfo = dockerService.getAllProfileStatuses().get(profile.getName());

      // Only proceed if container is running and auto-update is enabled
      if (containerInfo != null
          && containerInfo.getStatus() == ProfileStatus.RUNNING
          && Boolean.TRUE.equals(profile.getAutoUpdate())) {

        // Retrieve the previous image ID and current image name from the profile
        String previousImageId = profile.getLastImageId();
        String imageName = profile.getImage();

        // Ensure imageName is not null or empty
        if (imageName != null && !imageName.isEmpty()) {
          // Retrieve the latest image ID for the remote image
          dockerClient.pullImageCmd(imageName).start().awaitCompletion(); // Pull image if necessary
          String latestImageId = dockerClient.inspectImageCmd(imageName).exec().getId();
          LOG.info("Latest imageId is {}", latestImageId);

          // Check if the image has changed
          if (dockerService.hasImageIdChanged(profile.getName(), previousImageId, latestImageId)) {
            LOG.info("Image updated for '{}', restarting...", profile.getName());
            dockerService.startProfile(profile.getName());
          } else {
            LOG.info("No image update for '{}', skipping restart", profile.getName());
          }
        } else {
          LOG.error(
              "Image name is null or empty for profile '{}'. Skipping update.", profile.getName());
        }
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt(); // Preserve interrupt status
      LOG.error("Thread interrupted while updating profile '{}'", profile.getName(), ie);
    } catch (Exception e) {
      LOG.error("Error while checking profile '{}': {}", profile.getName(), e.getMessage(), e);
    }
  }
}
