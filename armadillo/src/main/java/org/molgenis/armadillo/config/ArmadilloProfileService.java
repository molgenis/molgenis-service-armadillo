package org.molgenis.armadillo.config;

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
import org.molgenis.armadillo.exceptions.CannotConnectToDockerException;
import org.molgenis.armadillo.metadata.ProfileDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ArmadilloProfileService {
  public static final String ARMADILLO_PROFILE = "org.molgenis.armadillo.profile";
  // remote control for docker
  private DockerClient dockerClient;

  public ArmadilloProfileService(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public ProfileDetails.Status getProfileStatus(ProfileDetails profileDetails) {
    try {
      InspectContainerResponse containerInfo =
          dockerClient.inspectContainerCmd(profileDetails.getName()).exec();
      if (containerInfo.getState().getRunning()) {
        return ProfileDetails.Status.RUNNING;
      } else {
        return ProfileDetails.Status.STOPPED;
      }
    } catch (Exception e) {
      throw new CannotConnectToDockerException(profileDetails, e);
    }
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void startProfile(ProfileDetails profileDetails) throws InterruptedException {
    // stop previous image if running
    this.removeProfile(profileDetails.getName());

    // load the image if needed
    dockerClient
        .pullImageCmd(profileDetails.getImage())
        .exec(new PullImageResultCallback())
        .awaitCompletion(5, TimeUnit.MINUTES);

    // start the image
    ExposedPort exposed = ExposedPort.tcp(6311);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(profileDetails.getPort()));
    CreateContainerResponse container;
    try (CreateContainerCmd cmd = dockerClient.createContainerCmd(profileDetails.getImage())) {
      container =
          cmd
              // mapping the port
              .withExposedPorts(exposed)
              .withHostConfig(new HostConfig().withPortBindings(portBindings))
              // mapping the name
              .withName(profileDetails.getName())
              // environment
              .withEnv("DEBUG=FALSE")
              .exec();
    }
    dockerClient.startContainerCmd(container.getId()).exec();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void removeProfile(String profileName) {
    try {
      dockerClient.stopContainerCmd(profileName).exec();
      dockerClient.removeContainerCmd(profileName).exec();
    } catch (NotFoundException nfe) {
      // no problem, might not exist anymore
    }
  }
}
