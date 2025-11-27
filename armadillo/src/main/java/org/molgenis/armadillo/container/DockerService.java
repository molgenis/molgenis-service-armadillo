package org.molgenis.armadillo.container;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.PullResponseItem;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.text.StringEscapeUtils;
import org.molgenis.armadillo.exceptions.*;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.armadillo.metadata.ContainerStatus;
import org.molgenis.armadillo.model.DockerImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class DockerService {

  private static final Logger LOG = LoggerFactory.getLogger(DockerService.class);

  private final DockerClient dockerClient;
  private final ContainerService containerService;
  private final ContainerStatusService containerStatusService;

  @Value("${armadillo.docker-run-in-container:false}")
  private boolean inContainer;

  @Value("${armadillo.container-prefix:''}")
  private String containerPrefix;

  public DockerService(
      DockerClient dockerClient,
      ContainerService containerService,
      ContainerStatusService containerStatusService) {
    this.dockerClient = dockerClient;
    this.containerService = containerService;
    this.containerStatusService = containerStatusService;
  }

  public Map<String, ContainerInfo> getAllContainerStatuses() {
    var names = containerService.getAll().stream().map(ContainerConfig::getName).toList();

    var statuses =
        names.stream()
            .collect(toMap(name -> name, name -> ContainerInfo.create(ContainerStatus.NOT_FOUND)));

    try {
      dockerClient
          .listContainersCmd()
          .withShowAll(true)
          .withNameFilter(names)
          .exec()
          .forEach(
              container ->
                  statuses.replace(
                      container.getNames()[0].substring(1),
                      ContainerInfo.create(
                          getImageTags(container.getImageId()),
                          ContainerStatus.of(container.getState()))));
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        statuses.replaceAll((key, value) -> ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE));
      } else {
        throw e;
      }
    }
    return statuses;
  }

  // `docker container ps` show these name structure

  /**
   * The container can run in its own network/compose.
   *
   * <p>Both the containers and/or Armadillo can be part of a docker-compose.yml. You can check with
   * `docker container ps` to see this name structure.
   *
   * @param containerName the container name set by DataManager.
   * @return adjusted container name if applicable.
   */
  String asContainerName(String containerName) {
    if (!inContainer) {
      LOG.warn("Image not running in docker container: " + containerName);
      return containerName;
    }

    if (containerPrefix.isEmpty()) {
      LOG.error("Running in container without prefix: " + containerName);
      return containerName;
    }

    LOG.warn("Image running in docker container: " + containerName);
    return containerPrefix + containerName + "-1";
  }

  public String[] getContainerEnvironmentConfig(String containerName) {
    containerService.getByName(containerName);
    String dockerContainerName = asContainerName(containerName);
    InspectContainerResponse containerInfo =
        dockerClient.inspectContainerCmd(dockerContainerName).exec();
    return containerInfo.getConfig().getEnv();
  }

  public ContainerInfo getContainerStatus(String containerName) {
    // check container exists
    containerService.getByName(containerName);

    String dockerContainerName = asContainerName(containerName);
    try {
      InspectContainerResponse containerInfo =
          dockerClient.inspectContainerCmd(dockerContainerName).exec();
      var tags = getImageTags(containerInfo.getName());
      return ContainerInfo.create(tags, ContainerStatus.of(containerInfo.getState()));
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        return ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE);
      } else {
        throw e;
      }
    } catch (NotFoundException e) {
      return ContainerInfo.create(ContainerStatus.NOT_FOUND);
    }
  }

  public void pullImageStartContainer(String containerName) {
    String dockerContainerName = asContainerName(containerName);
    LOG.info(containerName + " : " + dockerContainerName);

    var containerConfig = containerService.getByName(containerName);
    containerStatusService.updateStatus(containerName, null, null, null);
    pullImage(containerConfig);
    containerStatusService.updateStatus(containerName, "Profile installed", null, null);
    stopContainer(dockerContainerName);
    removeContainer(dockerContainerName);
    installImage(containerConfig);
    startContainer(dockerContainerName);

    String previousImageId = containerConfig.getLastImageId();
    String currentImageId =
        dockerClient.inspectContainerCmd(asContainerName(containerName)).exec().getImageId();

    if (previousImageId == null) {
      LOG.info(
          "No previous image ID recorded for {}. This may be the first run or from before image tracking was added.",
          containerName);
    } else if (hasImageIdChanged(containerName, previousImageId, currentImageId)) {
      try {
        deleteImageIfUnused(previousImageId);
      } catch (ImageRemoveFailedException e) {
        LOG.info(e.getMessage());
      }
    }

    updateImageMetaData(containerName, previousImageId, currentImageId);
  }

  void updateImageMetaData(String containerName, String previousImageId, String currentImageId) {
    String openContainersId = getOpenContainersImageVersion(currentImageId);
    Long imageSize = getImageSize(currentImageId);
    String creationDate = getImageCreationDate(currentImageId);

    String installDate;
    if ((previousImageId == null && currentImageId != null)
        || hasImageIdChanged(containerName, previousImageId, currentImageId)) {
      installDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    } else {
      installDate = null;
    }
    containerService.updateImageMetaData(
        containerName, currentImageId, openContainersId, imageSize, creationDate, installDate);
  }

  String getImageCreationDate(String imageId) {
    try {
      return dockerClient
          .inspectImageCmd(imageId)
          .exec()
          .getConfig()
          .getLabels()
          .get("org.opencontainers.image.created");
    } catch (Exception e) {
      LOG.error("Error retrieving creation date of image: {}", imageId, e);
      return null;
    }
  }

  Long getImageSize(String imageId) {
    try {
      return dockerClient.inspectImageCmd(imageId).exec().getSize();
    } catch (Exception e) {
      LOG.error("Error retrieving size of image: {}", imageId, e);
      return null;
    }
  }

  public String getOpenContainersImageVersion(String imageName) {
    try {
      // Inspect the image using the provided image name
      InspectImageResponse image = dockerClient.inspectImageCmd(imageName).exec();

      // Retrieve the OpenContainers image version (org.opencontainers.image.version)
      String imageVersion = image.getConfig().getLabels().get("org.opencontainers.image.version");

      // Return the version if available, or null if not found
      return imageVersion != null ? imageVersion : "Unknown Version";
    } catch (Exception e) {
      // Log the error and return null if the image couldn't be inspected
      LOG.error("Error retrieving OpenContainers version for image: {}", imageName, e);
      return null;
    }
  }

  boolean hasImageIdChanged(String containerName, String previousImageId, String currentImageId) {
    String escapedContainer = StringEscapeUtils.escapeJava(containerName);
    String escapedPreviousId =
        previousImageId != null ? StringEscapeUtils.escapeJava(previousImageId) : null;
    String escapedCurrentId = StringEscapeUtils.escapeJava(currentImageId);

    if (previousImageId != null && !previousImageId.equals(currentImageId)) {
      LOG.info(
          "Image ID for container '{}' changed from '{}' to '{}'",
          escapedContainer,
          escapedPreviousId,
          escapedCurrentId);
      return true;
    } else {
      LOG.info(
          "Image ID for container '{}' unchanged (still '{}')", escapedContainer, escapedCurrentId);
      return false;
    }
  }

  void installImage(ContainerConfig containerConfig) {
    if (containerConfig.getImage() == null) {
      throw new MissingImageException(containerConfig.getImage());
    }

    // if rock is in the image name, it's rock
    int imageExposed = containerConfig.getImage().contains("rock") ? 8085 : 6311;
    ExposedPort exposed = ExposedPort.tcp(imageExposed);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(containerConfig.getPort()));
    try (CreateContainerCmd cmd = dockerClient.createContainerCmd(containerConfig.getImage())) {
      cmd.withExposedPorts(exposed)
          .withHostConfig(
              new HostConfig()
                  .withPortBindings(portBindings)
                  .withRestartPolicy(RestartPolicy.unlessStoppedRestart()))
          .withName(containerConfig.getName())
          .withEnv("DEBUG=FALSE")
          .exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(containerConfig.getImage(), e);
    }
  }

  private void startContainer(String containerName) {
    try {
      dockerClient.startContainerCmd(containerName).exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(containerName, e);
    }
  }

  private void stopContainer(String dockerContainerName) {
    String containerIdForLog =
        "stoppingContainer has not containerIdForLog: " + dockerContainerName;
    try {
      dockerClient.stopContainerCmd(dockerContainerName).exec();
    } catch (DockerException e) {
      try {
        InspectContainerResponse containerInfo =
            dockerClient.inspectContainerCmd(dockerContainerName).exec();
        // should not be a problem if not running
        if (TRUE.equals(containerInfo.getState().getRunning())) {
          throw new ImageStopFailedException(containerIdForLog, e);
        }
      } catch (NotFoundException nfe) {
        LOG.info("Failed to stop container '{}' because it doesn't exist", containerIdForLog);
        // not a problem, its gone
      } catch (Exception e2) {
        throw new ImageStopFailedException(containerIdForLog, e);
      }
    } catch (Exception e) {
      throw new ImageStopFailedException(containerIdForLog, e);
    }
  }

  private void pullImage(ContainerConfig containerConfig) {
    if (containerConfig.getImage() == null) {
      throw new MissingImageException(containerConfig.getName());
    }

    try {
      dockerClient
          .pullImageCmd(containerConfig.getImage())
          .exec(getPullProgress(containerConfig))
          .awaitCompletion(10, TimeUnit.MINUTES);

    } catch (NotFoundException e) {
      throw new ImagePullFailedException(containerConfig.getImage(), e);
    } catch (RuntimeException e) {
      LOG.warn("Couldn't pull image", e);
      // Typically, network offline; for local use we can continue.
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ImagePullFailedException(containerConfig.getImage(), e);
    }
  }

  private PullImageResultCallback getPullProgress(ContainerConfig containerConfig) {
    final Set<String> seen = ConcurrentHashMap.newKeySet();
    final Set<String> done = ConcurrentHashMap.newKeySet();
    final AtomicInteger lastPct = new AtomicInteger(0);

    return new PullImageResultCallback() {
      @Override
      public void onNext(PullResponseItem item) {
        final String id = item.getId();
        if (id == null) {
          super.onNext(item);
          return;
        }

        seen.add(id);
        final String status = String.valueOf(item.getStatus());

        if ("Pull complete".equalsIgnoreCase(status) || "Already exists".equalsIgnoreCase(status)) {
          done.add(id);
        }

        int completed = done.size();
        int total = Math.max(1, seen.size());

        containerStatusService.updateStatus(
            containerConfig.getName(), "Installing container", completed, total);

        super.onNext(item);
      }
    };
  }

  public void stopAndRemoveContainer(String containerName) {
    // check container exists
    containerService.getByName(containerName);
    stopContainer(containerName);
    removeContainer(containerName);
  }

  public void removeContainerDeleteImage(String containerName) {
    stopAndRemoveContainer(containerName);
    String imageId = containerService.getByName(containerName).getLastImageId();
    try {
      deleteImageIfUnused(imageId);
    } catch (ImageRemoveFailedException e) {
      LOG.info(e.getMessage());
    }
  }

  private void removeContainer(String containerName) {
    try {
      dockerClient.removeContainerCmd(containerName).exec();
    } catch (NotFoundException nfe) {
      LOG.info("Couldn't remove container '{}' because it doesn't exist", containerName);
      // not a problem, wanted to remove anyway
    } catch (DockerException e) {
      throw new ContainerRemoveFailedException(containerName, e);
    }
  }

  List<String> getImageTags(String imageId) {
    try {
      return dockerClient.inspectImageCmd(imageId).exec().getRepoTags();
    } catch (DockerException e) {
      if (e instanceof NotFoundException) {
        LOG.warn("Couldn't inspect image, because: " + e.getMessage());
      } else {
        LOG.warn("Couldn't inspect image", e);
        // getting image tags is non-essential, don't throw error
      }
    }
    return emptyList();
  }

  public List<DockerImageInfo> getDockerImages() {
    return dockerClient.listImagesCmd().withShowAll(TRUE).exec().stream()
        .map(DockerImageInfo::create)
        .toList();
  }

  public void deleteImageIfUnused(String imageId) {
    if (imageId == null) {
      LOG.info("No image ID provided; skipping image removal");
      return;
    }

    String safeImageId = StringEscapeUtils.escapeJava(imageId);

    try {
      boolean isInUse =
          dockerClient.listContainersCmd().exec().stream()
              .anyMatch(container -> Objects.equals(container.getImageId(), imageId));

      if (isInUse) {
        LOG.info("Image ID '{}' is still in use — skipping removal", safeImageId);
        throw new ImageRemoveFailedException(
            safeImageId, "Image ID is still in use — skipping removal");
      }
      dockerClient.removeImageCmd(imageId).withForce(true).exec();
      LOG.info("Removed image ID '{}' from local Docker cache", safeImageId);
    } catch (NotFoundException e) {
      throw new ImageRemoveFailedException(safeImageId, "Image ID not found — skipping removal");
    }
  }
}
