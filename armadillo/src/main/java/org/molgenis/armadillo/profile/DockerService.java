package org.molgenis.armadillo.profile;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.commons.text.StringEscapeUtils;
import org.molgenis.armadillo.exceptions.*;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
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
  private final ProfileService profileService;

  @Value("${armadillo.docker-run-in-container:false}")
  private boolean inContainer;

  @Value("${armadillo.container-prefix:''}")
  private String containerPrefix;

  public DockerService(DockerClient dockerClient, ProfileService profileService) {
    this.dockerClient = dockerClient;
    this.profileService = profileService;
  }

  public Map<String, ContainerInfo> getAllProfileStatuses() {
    var names = profileService.getAll().stream().map(ProfileConfig::getName).toList();

    var statuses =
        names.stream()
            .collect(toMap(name -> name, name -> ContainerInfo.create(ProfileStatus.NOT_FOUND)));

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
                          ProfileStatus.of(container.getState()))));
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        statuses.replaceAll((key, value) -> ContainerInfo.create(ProfileStatus.DOCKER_OFFLINE));
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
   * <p>Both the profiles and/or Armadillo can be part of a docker-compose.yml. You can check with
   * `docker container ps` to see this name structure.
   *
   * @param profileName the profile name set by DataManager.
   * @return adjusted container name if applicable.
   */
  String asContainerName(String profileName) {
    if (!inContainer) {
      LOG.warn("Profile not running in docker container: " + profileName);
      return profileName;
    }

    if (containerPrefix.isEmpty()) {
      LOG.error("Running in container without prefix: " + profileName);
      return profileName;
    }

    LOG.warn("Profile running in docker container: " + profileName);
    return containerPrefix + profileName + "-1";
  }

  String asProfileName(String containerName) {
    if (inContainer) {
      return containerName.replace("armadillo-docker-compose-", "").replace("-1", "");
    }
    return containerName;
  }

  public String[] getProfileEnvironmentConfig(String profileName) {
    profileService.getByName(profileName);
    String containerName = asContainerName(profileName);
    InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
    return containerInfo.getConfig().getEnv();
  }

  public ContainerInfo getProfileStatus(String profileName) {
    // check profile exists
    profileService.getByName(profileName);

    String containerName = asContainerName(profileName);
    try {
      InspectContainerResponse containerInfo =
          dockerClient.inspectContainerCmd(containerName).exec();
      var tags = getImageTags(containerInfo.getName());
      return ContainerInfo.create(tags, ProfileStatus.of(containerInfo.getState()));
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        return ContainerInfo.create(ProfileStatus.DOCKER_OFFLINE);
      } else {
        throw e;
      }
    } catch (NotFoundException e) {
      return ContainerInfo.create(ProfileStatus.NOT_FOUND);
    }
  }

  public void startProfile(String profileName) {
    String containerName = asContainerName(profileName);
    LOG.info(profileName + " : " + containerName);

    var profileConfig = profileService.getByName(profileName);
    pullImage(profileConfig);
    stopContainer(containerName);
    removeContainer(containerName); // for reinstall
    installImage(profileConfig);
    startContainer(containerName);

    String previousImageId = profileConfig.getLastImageId();
    String currentImageId =
        dockerClient.inspectContainerCmd(asContainerName(profileName)).exec().getImageId();

    if (previousImageId == null) {
      LOG.info(
          "No previous image ID recorded for {}. This may be the first run or from before image tracking was added.",
          profileName);
    } else if (hasImageIdChanged(profileName, previousImageId, currentImageId)) {
      try {
        removeImageIfUnused(previousImageId);
      } catch (ImageRemoveFailedException e) {
        LOG.info(e.getMessage());
      }
    }
  }

    updateImageMetaData(profileName, previousImageId, currentImageId);
  }

  void updateImageMetaData(String profileName, String previousImageId, String currentImageId) {
    String openContainersId = getOpenContainersImageVersion(currentImageId);
    Long imageSize = getImageSize(currentImageId);
    String creationDate = getImageCreationDate(currentImageId);

    String installDate;
    if ((previousImageId == null && currentImageId != null)
        || hasImageIdChanged(profileName, previousImageId, currentImageId)) {
      installDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    } else {
      installDate = null;
    }
    System.out.println("Previous imageId is: " + previousImageId);
    System.out.println("Current imageId is: " + currentImageId);
    System.out.println("Install date is: " + installDate);
    profileService.updateImageMetaData(
        profileName, currentImageId, openContainersId, imageSize, creationDate, installDate);
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

  boolean hasImageIdChanged(String profileName, String previousImageId, String currentImageId) {
    String escapedProfile = StringEscapeUtils.escapeJava(profileName);
    String escapedPreviousId =
        previousImageId != null ? StringEscapeUtils.escapeJava(previousImageId) : null;
    String escapedCurrentId = StringEscapeUtils.escapeJava(currentImageId);

    if (previousImageId != null && !previousImageId.equals(currentImageId)) {
      LOG.info(
          "Image ID for profile '{}' changed from '{}' to '{}'",
          escapedProfile,
          escapedPreviousId,
          escapedCurrentId);
      return true;
    } else {
      LOG.info(
          "Image ID for profile '{}' unchanged (still '{}')", escapedProfile, escapedCurrentId);
      return false;
    }
  }

  void installImage(ProfileConfig profileConfig) {
    if (profileConfig.getImage() == null) {
      throw new MissingImageException(profileConfig.getImage());
    }

    // if rock is in the image name, it's rock
    int imageExposed = profileConfig.getImage().contains("rock") ? 8085 : 6311;
    ExposedPort exposed = ExposedPort.tcp(imageExposed);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(profileConfig.getPort()));
    try (CreateContainerCmd cmd = dockerClient.createContainerCmd(profileConfig.getImage())) {
      cmd.withExposedPorts(exposed)
          .withHostConfig(
              new HostConfig()
                  .withPortBindings(portBindings)
                  .withRestartPolicy(RestartPolicy.unlessStoppedRestart()))
          .withName(profileConfig.getName())
          .withEnv("DEBUG=FALSE")
          .exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(profileConfig.getImage(), e);
    }
  }

  private void startContainer(String containerName) {
    try {
      dockerClient.startContainerCmd(containerName).exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(containerName, e);
    }
  }

  private void stopContainer(String containerName) {
    String profileName = "stoppingContainer has not profileName: " + containerName;
    try {
      dockerClient.stopContainerCmd(containerName).exec();
    } catch (DockerException e) {
      try {
        InspectContainerResponse containerInfo =
            dockerClient.inspectContainerCmd(containerName).exec();
        // should not be a problem if not running
        if (TRUE.equals(containerInfo.getState().getRunning())) {
          throw new ImageStopFailedException(profileName, e);
        }
      } catch (NotFoundException nfe) {
        LOG.info("Failed to stop profile '{}' because it doesn't exist", profileName);
        // not a problem, its gone
      } catch (Exception e2) {
        throw new ImageStopFailedException(profileName, e);
      }
    } catch (Exception e) {
      throw new ImageStopFailedException(profileName, e);
    }
  }

  private void pullImage(ProfileConfig profileConfig) {
    if (profileConfig.getImage() == null) {
      throw new MissingImageException(profileConfig.getName());
    }

    try {
      dockerClient
          .pullImageCmd(profileConfig.getImage())
          .exec(new PullImageResultCallback())
          .awaitCompletion(5, TimeUnit.MINUTES);
    } catch (NotFoundException e) {
      throw new ImagePullFailedException(profileConfig.getImage(), e);
    } catch (RuntimeException e) {
      LOG.warn("Couldn't pull image", e);
      // typically, network offline, for local use we can continue.
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ImagePullFailedException(profileConfig.getImage(), e);
    }
  }

  public void removeProfile(String profileName) {
    // check profile exists
    profileService.getByName(profileName);
    stopContainer(profileName);
    removeContainer(profileName);
  }

  public void deleteProfile(String profileName) {
    removeProfile(profileName);
    String imageId = profileService.getByName(profileName).getLastImageId();
    try {
      removeImageIfUnused(imageId);
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
      LOG.warn("Couldn't inspect image", e);
      // getting image tags is non-essential, don't throw error
    }
    return emptyList();
  }

  public List<DockerImageInfo> getDockerImages() {
    return dockerClient.listImagesCmd().withShowAll(TRUE).exec().stream()
        .map(DockerImageInfo::create)
        .toList();
  }

  public void removeImageIfUnused(String imageId) {
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
