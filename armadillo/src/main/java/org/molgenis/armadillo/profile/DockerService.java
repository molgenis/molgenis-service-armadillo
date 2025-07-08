package org.molgenis.armadillo.profile;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;
import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.molgenis.armadillo.exceptions.*;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
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
    String imageName = profileService.getByName(profileName).getImage();
    removeImageIfUnused(imageName);
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

  private List<String> getImageTags(String imageId) {
    try {
      return dockerClient.inspectImageCmd(imageId).exec().getRepoTags();
    } catch (DockerException e) {
      LOG.warn("Couldn't inspect image", e);
      // getting image tags is non-essential, don't throw error
    }
    return emptyList();
  }

  private void removeImageIfUnused(String imageName) {
    // Find image ID for given name
    String imageId = dockerClient.inspectImageCmd(imageName).exec().getId();

    // Check if any containers are using this image ID
    boolean isInUse =
        dockerClient.listContainersCmd().withShowAll(true).exec().stream()
            .anyMatch(container -> container.getImageId().equals(imageId));

    if (isInUse) {
      LOG.info("Image '{}' (ID: {}) still in use — skipping removal", imageName, imageId);
      return;
    }

    // Remove all tags pointing to this image
    List<String> tags = dockerClient.inspectImageCmd(imageId).exec().getRepoTags();
    for (String tag : tags) {
      dockerClient.removeImageCmd(tag).withForce(true).exec();
      LOG.info("Removed image tag '{}'", tag);
    }

    LOG.info("Fully removed image ID: {}", imageId);
  }
}
