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
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
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

  // FIXME: Get from environment
  Boolean inContainer() {
    return true;
  }

  // `docker container ps` show these name structure
  String asContainerName(String profileName) {
    if (inContainer()) {
      return "armadillo-docker-compose-" + profileName + "-1";
    }
    return profileName;
  }

  String asProfileName(String containerName) {
    if (inContainer()) {
      return containerName.replace("armadillo-docker-compose-", "").replace("-1", "");
    }
    return containerName;
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

  private void installImage(ProfileConfig profileConfig) {
    if (profileConfig.getImage() == null) {
      throw new MissingImageException(profileConfig.getImage());
    }

    int imageExposed = 8085;
    ExposedPort exposed = ExposedPort.tcp(imageExposed);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(profileConfig.getPort()));
    try (CreateContainerCmd cmd = dockerClient.createContainerCmd(profileConfig.getImage())) {
      cmd.withExposedPorts(exposed)
          .withHostConfig(new HostConfig().withPortBindings(portBindings))
          .withName(profileConfig.getName())
          .withEnv("DEBUG=FALSE")
          .exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(profileConfig.getImage(), e);
    }
  }

  private void startContainer(String profileName) {
    try {
      dockerClient.startContainerCmd(asContainerName(profileName)).exec();
    } catch (DockerException e) {
      throw new ImageStartFailedException(profileName, e);
    }
  }

  private void stopContainer(String profileName) {
    try {
      dockerClient.stopContainerCmd(asContainerName(profileName)).exec();
    } catch (DockerException e) {
      try {
        InspectContainerResponse containerInfo =
            dockerClient.inspectContainerCmd(asContainerName(profileName)).exec();
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

  private void removeContainer(String profileName) {
    try {
      dockerClient.removeContainerCmd(asContainerName(profileName)).exec();
    } catch (NotFoundException nfe) {
      LOG.info("Couldn't remove container '{}' because it doesn't exist", profileName);
      // not a problem, wanted to remove anyway
    } catch (DockerException e) {
      throw new ContainerRemoveFailedException(profileName, e);
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
}
