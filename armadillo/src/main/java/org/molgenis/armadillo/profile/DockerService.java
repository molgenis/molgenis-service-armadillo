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
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.ProcessingException;
import org.molgenis.armadillo.exceptions.MissingImageException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class DockerService {

  private final DockerClient dockerClient;
  private final ProfileService profileService;

  public DockerService(DockerClient dockerClient, ProfileService profileService) {
    this.dockerClient = dockerClient;
    this.profileService = profileService;
  }

  public Map<String, ProfileStatus> getAllProfileStatuses() {
    var names = profileService.getAll().stream().map(ProfileConfig::getName).toList();
    var statuses =
        names.stream().collect(Collectors.toMap(name -> name, name -> ProfileStatus.NOT_FOUND));

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
                      ProfileStatus.ofDockerStatus(container.getState())));
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        statuses.replaceAll((key, value) -> ProfileStatus.DOCKER_OFFLINE);
      } else {
        throw e;
      }
    }
    return statuses;
  }

  public ProfileStatus getProfileStatus(String profileName) {
    // check profile exists
    profileService.getByName(profileName);

    try {
      InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(profileName).exec();
      if (TRUE.equals(containerInfo.getState().getRunning())) {
        return ProfileStatus.RUNNING;
      } else {
        return ProfileStatus.STOPPED;
      }
    } catch (ProcessingException e) {
      if (e.getCause() instanceof SocketException) {
        return ProfileStatus.DOCKER_OFFLINE;
      } else {
        throw e;
      }
    }
  }

  public void startProfile(String profileName) {
    var profileConfig = profileService.getByName(profileName);

    if (profileConfig.getImage() == null) {
      throw new MissingImageException(profileName);
    }

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

  public void stopProfile(String profileName) {
    // TODO
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
