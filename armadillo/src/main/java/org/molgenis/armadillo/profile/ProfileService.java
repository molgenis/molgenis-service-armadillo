package org.molgenis.armadillo.profile;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);

  // remote control for docker
  private DockerClient dockerClient;
  // get if docker management is enabled from config file
  private boolean dockerManagementEnabled;

  public ProfileService(
      DockerClient dockerClient,
      @Value("${datashield.docker-management-enabled}") boolean dockerManagementEnabled) {
    this.dockerClient = dockerClient;
    this.dockerManagementEnabled = dockerManagementEnabled;
  }

  public ProfileStatus getProfileStatus(ProfileConfig profileConfig) {
    if (dockerManagementEnabled) {
      try {
        InspectContainerResponse containerInfo =
            dockerClient.inspectContainerCmd(profileConfig.getName()).exec();
        if (containerInfo.getState().getRunning()) {
          return ProfileStatus.RUNNING;
        } else {
          return ProfileStatus.STOPPED;
        }
      } catch (Exception e) {
        // if this fails we could try to connect to the server without docker client
        // that would also work in case the profiles would be managed by hand.
        // proposal: make this depend on a configuration option whether to manage docker manually.
        if (e.getMessage().contains("Connection refused")) {
          return ProfileStatus.CONNECTION_REFUSED;
        } else {
          throw e;
        }
      }
    } else {
      LOGGER.info(
          String.format(
              "skipped get profile status docker for profile %s because datashield.docker-management-enabled=%s",
              profileConfig.getName(), this.dockerManagementEnabled));
      return ProfileStatus.DOCKER_MANAGEMENT_DISABLED;
    }
  }

  public void startProfile(ProfileConfig profileConfig) throws InterruptedException {
    if (dockerManagementEnabled) {
      // stop previous image if running
      this.removeProfile(profileConfig.getName());

      // load the image if needed
      dockerClient
          .pullImageCmd(profileConfig.getImage())
          .exec(new PullImageResultCallback())
          .awaitCompletion(5, TimeUnit.MINUTES);

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
    } else {
      LOGGER.info(
          String.format(
              "skipped start profile %s because datashield.docker-management-enabled=%s",
              profileConfig.getName(), this.dockerManagementEnabled));
    }
  }

  public void removeProfile(String profileName) {
    if (dockerManagementEnabled) {
      try {
        dockerClient.stopContainerCmd(profileName).exec();
        dockerClient.removeContainerCmd(profileName).exec();
      } catch (NotFoundException nfe) {
        // no problem, might not exist anymore
        // idempotent :-)
      }
    } else {
      LOGGER.info(
          String.format(
              "skipped remove docker for profile %s because datashield.docker-management-enabled=%s",
              profileName, this.dockerManagementEnabled));
    }
  }
}
