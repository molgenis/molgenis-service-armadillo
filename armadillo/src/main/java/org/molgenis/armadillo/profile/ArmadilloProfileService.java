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
import org.springframework.stereotype.Service;

@Service
public class ArmadilloProfileService {
  // remote control for docker
  private DockerClient dockerClient;

  public ArmadilloProfileService(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public ProfileStatus getProfileStatus(ProfileConfig profileConfig) {
    try {
      InspectContainerResponse containerInfo =
          dockerClient.inspectContainerCmd(profileConfig.getName()).exec();
      if (containerInfo.getState().getRunning()) {
        return ProfileStatus.RUNNING;
      } else {
        return ProfileStatus.STOPPED;
      }
    } catch (Exception e) {
      if (e.getMessage().contains("Connection refused")) {
        return ProfileStatus.CONNECTION_REFUSED;
      } else {
        throw e;
      }
    }
  }

  public void startProfile(ProfileConfig profileConfig) throws InterruptedException {
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
