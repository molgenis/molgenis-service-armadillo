package org.molgenis.armadillo.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DatashieldProfileManager {
  public static final String ARMADILLO_PROFILE = "org.molgenis.armadillo.profile";
  public static final String ARMADILLO_WHITELIST = "org.molgenis.armadillo.whitelist";
  // remote control for docker
  private static final DefaultDockerClientConfig.Builder config =
      DefaultDockerClientConfig.createDefaultConfigBuilder();
  private static final DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();

  public List<ProfileConfigProps> listDatashieldProfiles() {
    List<ProfileConfigProps> result =
        dockerClient.listContainersCmd().exec().stream()
            .filter(container -> container.getLabels().containsKey(ARMADILLO_PROFILE))
            .map(
                container -> {
                  ProfileConfigProps def = new ProfileConfigProps();
                  def.setDockerImage(container.getImage());
                  def.setName(container.getLabels().get(ARMADILLO_PROFILE));
                  def.setWhiteList(
                      Set.of(container.getLabels().get(ARMADILLO_WHITELIST).split(",")));
                  // below code is a bit expansive and possibly brittle?
                  // however, systems seems to cache because it is fast on my machine
                  InspectContainerResponse containerInfo =
                      dockerClient.inspectContainerCmd(container.getNames()[0]).exec();
                  def.setPort(
                      Integer.parseInt(
                          containerInfo
                              .getHostConfig()
                              .getPortBindings()
                              .getBindings()
                              .get(new ExposedPort(6311))[0]
                              .getHostPortSpec()));
                  def.getOptions();
                  return def;
                })
            .collect(Collectors.toList());
    return result;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void addDockerProfile(ProfileConfigProps props) throws InterruptedException {
    // stop previous image if running
    this.removeDockerProfile(props.getName());

    // load the image if needed
    dockerClient
        .pullImageCmd(props.getDockerImage())
        .exec(new PullImageResultCallback())
        .awaitCompletion(5, TimeUnit.MINUTES);

    // start the image
    ExposedPort exposed = ExposedPort.tcp(6311);
    Ports portBindings = new Ports();
    portBindings.bind(exposed, Ports.Binding.bindPort(props.getPort()));
    CreateContainerResponse container =
        dockerClient
            .createContainerCmd(props.getDockerImage())
            // nice group name in docker, and add the profile label and whitelist
            .withLabels(
                Map.of(
                    ARMADILLO_PROFILE,
                    props.getName(),
                    ARMADILLO_WHITELIST,
                    String.join(",", props.getWhitelist())))
            // mapping the port
            .withExposedPorts(exposed)
            .withHostConfig(new HostConfig().withPortBindings(portBindings))
            // mapping the name
            .withName(props.getName())
            // environment
            .withEnv("DEBUG=FALSE")
            .exec();
    dockerClient.startContainerCmd(container.getId()).exec();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void removeDockerProfile(String profileName) {
    Map<String, String> result = new LinkedHashMap<>();
    dockerClient
        .listContainersCmd()
        .exec()
        .forEach(
            container -> {
              // silly that we need to add "/" before
              if (container.getLabels() != null
                  && profileName.equals(container.getLabels().get(ARMADILLO_PROFILE))) {
                dockerClient.stopContainerCmd(container.getId()).exec();
                dockerClient.removeContainerCmd(container.getId()).exec();
                result.put("message", "Deleted armadillo profile: " + profileName);
              }
            });
    if (result.isEmpty()) {
      result.put("message", "Delete armadillo profile skipped: " + profileName + " did not exist");
    }
  }
}
