package org.molgenis.armadillo.container;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import com.github.dockerjava.api.DockerClient;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.molgenis.armadillo.controller.ContainerDockerController;
import org.molgenis.armadillo.metadata.ContainerStatus;
import org.molgenis.armadillo.metadata.UpdateSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(ContainerDockerController.DOCKER_MANAGEMENT_ENABLED)
public class ContainerScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(ContainerScheduler.class);

  private final DockerService dockerService;
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
  private ThreadPoolTaskScheduler taskScheduler;
  private final DockerClient dockerClient;

  public ContainerScheduler(DockerService dockerService, DockerClient dockerClient) {
    this.dockerService = dockerService;
    this.dockerClient = dockerClient;
  }

  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    this.taskScheduler = new ThreadPoolTaskScheduler();
    this.taskScheduler.setPoolSize(10);
    this.taskScheduler.setThreadNamePrefix("ContainerUpdater-");
    this.taskScheduler.initialize();
    return this.taskScheduler;
  }

  /** Reschedule or create a scheduled update task for a container. */
  public void reschedule(DatashieldContainerConfig container) {
    cancel(container.getName());

    if (Boolean.TRUE.equals(container.getAutoUpdate()) && container.getUpdateSchedule() != null) {
      String cron = toCron(container.getUpdateSchedule());
      Runnable task = () -> runAsSystem(() -> runUpdateForContainer(container));
      ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(cron));
      scheduledTasks.put(container.getName(), future);
      LOG.info(
          "Scheduled auto-update for container '{}' with cron '{}'", container.getName(), cron);
    }
  }

  /** Cancel the scheduled task for a given container name, if any. */
  public void cancel(String containerName) {
    ScheduledFuture<?> existing = scheduledTasks.remove(containerName);
    if (existing != null) {
      existing.cancel(false);
      LOG.info("Cancelled auto-update task for container '{}'", containerName);
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

  private void runUpdateForContainer(DatashieldContainerConfig container) {
    try {
      // Get container status directly using container name
      var containerInfo = dockerService.getAllContainerStatuses().get(container.getName());

      // Only proceed if container is running and auto-update is enabled
      if (containerInfo != null
          && containerInfo.getStatus() == ContainerStatus.RUNNING
          && Boolean.TRUE.equals(container.getAutoUpdate())) {

        // Retrieve the previous image ID and current image name from the container
        String previousImageId = container.getLastImageId();
        String imageName = container.getImage();

        // Ensure imageName is not null or empty
        if (imageName != null && !imageName.isEmpty()) {
          // Retrieve the latest image ID for the remote image
          dockerClient.pullImageCmd(imageName).start().awaitCompletion(); // Pull image if necessary
          String latestImageId = dockerClient.inspectImageCmd(imageName).exec().getId();
          LOG.info("Latest imageId is {}", latestImageId);

          // Check if the image has changed
          if (dockerService.hasImageIdChanged(
              container.getName(), previousImageId, latestImageId)) {
            LOG.info("Image updated for '{}', restarting...", container.getName());
            dockerService.pullImageStartContainer(container.getName());
          } else {
            LOG.info("No image update for '{}', skipping restart", container.getName());
          }
        } else {
          LOG.error(
              "Image name is null or empty for container '{}'. Skipping update.",
              container.getName());
        }
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt(); // Preserve interrupt status
      LOG.error("Thread interrupted while updating container '{}'", container.getName(), ie);
    } catch (Exception e) {
      LOG.error("Error while checking container '{}': {}", container.getName(), e.getMessage(), e);
    }
  }
}
