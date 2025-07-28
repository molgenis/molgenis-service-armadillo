package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import com.github.dockerjava.api.DockerClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.molgenis.armadillo.metadata.AutoUpdateSchedule;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
public class ProfileScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(ProfileScheduler.class);

  private final DockerService dockerService;
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
  private ThreadPoolTaskScheduler taskScheduler;
  private final ProfileService profileService;
  private final DockerClient dockerClient;

  public ProfileScheduler(
      DockerService dockerService, ProfileService profileService, DockerClient dockerClient) {
    this.dockerService = dockerService;
    this.profileService = profileService;
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
  public void reschedule(ProfileConfig profile) {
    cancel(profile.getName());

    if (Boolean.TRUE.equals(profile.getAutoUpdate()) && profile.getAutoUpdateSchedule() != null) {
      String cron = toCron(profile.getAutoUpdateSchedule());
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

  private String toCron(AutoUpdateSchedule schedule) {
    String time = schedule.time != null ? schedule.time : "01:00";
    String frequency = schedule.frequency != null ? schedule.frequency : "weekly";
    String day = schedule.day;

    String[] parts = time.split(":");
    String minute = parts[1];
    String hour = parts[0];

    if ("weekly".equalsIgnoreCase(frequency)) {
      int dayOfWeek = dayToCronNumber(day != null ? day : "Sunday");
      return String.format("0 %s %s * * %d", minute, hour, dayOfWeek);
    } else {
      return String.format("0 %s %s * * *", minute, hour); // daily or fallback
    }
  }

  private int dayToCronNumber(String day) {
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

  private void runUpdateForProfile(ProfileConfig profile) {
    String profileName = profile.getName();

    try {
      var containerInfo = dockerService.getAllProfileStatuses().get(profile.getName());

      // Only proceed if container is running and auto-update is enabled
      if (containerInfo != null
          && containerInfo.getStatus() == ProfileStatus.RUNNING
          && Boolean.TRUE.equals(profile.getAutoUpdate())) {

        var profileConfig = profileService.getByName(profileName);
        String previousImageId = profileConfig.getLastImageId();

        // Inspect the container only after confirming status
        String currentImageId =
            dockerClient
                .inspectContainerCmd(dockerService.asContainerName(profileName))
                .exec()
                .getImageId();

        if (dockerService.hasImageIdChanged(previousImageId, currentImageId)) {
          LOG.info("Image updated for '{}', restarting...", profileName);
          dockerService.startProfile(profileName);
        } else {
          LOG.info("No image update for '{}', skipping restart", profileName);
        }
      }
    } catch (Exception e) {
      LOG.error("Error while checking profile '{}': {}", profileName, e.getMessage(), e);
    }
  }
}
