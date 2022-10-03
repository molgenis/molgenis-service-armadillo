package org.molgenis.armadillo.profile;

import static java.lang.Boolean.TRUE;
import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import java.util.concurrent.TimeUnit;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class DockerService {
  private final DockerClient dockerClient;

  public DockerService(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public ProfileStatus getProfileStatus(String profileName) {
    try {
      InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(profileName).exec();
      if (TRUE.equals(containerInfo.getState().getRunning())) {
        return ProfileStatus.RUNNING;
      } else {
        return ProfileStatus.STOPPED;
      }
    } catch (Exception e) {
      // TODO catch DockerException instead of all exceptions

      // if this fails we could try to connect to the server without docker client
      // that would also work in case the profiles would be managed by hand.
      // proposal: make this depend on a configuration option whether to manage docker manually.
      if (e.getMessage().contains("Connection refused")) {
        return ProfileStatus.CONNECTION_REFUSED;
      } else {
        throw e;
      }
    }
  }

  public void startProfile(ProfileConfig profileConfig) {
    // stop previous image if running
    removeProfile(profileConfig.getName());

    // load the image if needed
    try {
      dockerClient
          .pullImageCmd(profileConfig.getImage())
          .exec(new PullImageResultCallback())
          .awaitCompletion(5, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      // TODO handle exception
      e.printStackTrace();
    }

    // start the image
    ExposedPort exposed = ExposedPort.tcp(6311);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(profileConfig.getPort()));
    CreateContainerResponse container;
    try (CreateContainerCmd cmd = dockerClient.createContainerCmd(profileConfig.getImage())) {
      container =
          cmd
              // mapping the port
              .withExposedPorts(exposed)
              .withHostConfig(new HostConfig().withPortBindings(portBindings))
              // mapping the name
              .withName(profileConfig.getName())
              // environment
              .withEnv("DEBUG=FALSE")
              .exec();
    }
    dockerClient.startContainerCmd(container.getId()).exec();
  }

  public void removeProfile(String profileName) {
    try {
      dockerClient.stopContainerCmd(profileName).exec();
      dockerClient.removeContainerCmd(profileName).exec();
    } catch (NotFoundException nfe) {
      // no problem, might not exist anymore
      // idempotent :-)
    }
  }
}
