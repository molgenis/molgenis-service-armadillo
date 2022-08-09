package org.molgenis.armadillo.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DatashieldProfileManager {
  public static final String ARMADILLO_PROFILE = "org.molgenis.armadillo.profile";
  public static final String ARMADILLO_WHITELIST = "org.molgenis.armadillo.whitelist";
  // remote control for docker
  DefaultDockerClientConfig.Builder config = DefaultDockerClientConfig.createDefaultConfigBuilder();
  private DockerClient dockerClient;

  public DatashieldProfileManager() {
    dockerClient = DockerClientBuilder.getInstance(config).build();
  }

  public List<ProfileConfigProps> listDockers() {
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
                  // def.setPort(container.getPorts()[0].getPublicPort());
                  return def;
                })
            .collect(Collectors.toList());
    return result;
  }

  public void addDockerProfile(ProfileConfigProps props) throws InterruptedException {
    // stop previous image if running
    this.removeDockerProfile(props.getName());

    // load the image if needed
    dockerClient
        .pullImageCmd(props.getDockerImage())
        .exec(new PullImageResultCallback())
        .awaitCompletion(5, TimeUnit.MINUTES);

    // start the image
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
            .withExposedPorts(new ExposedPort(props.getPort()))
            // mapping the name
            .withName(props.getName())
            .exec();
    dockerClient.startContainerCmd(container.getId()).exec();
  }

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
