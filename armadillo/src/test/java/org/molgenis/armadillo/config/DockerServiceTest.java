package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.molgenis.armadillo.metadata.ProfileStatus.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.profile.DockerService;

@ExtendWith(MockitoExtension.class)
public class DockerServiceTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  InspectContainerResponse containerInfo;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DockerClient dockerClient;

  @Mock ProfileService profileService;

  @Mock PullImageCmd pullImageCmd;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  PullImageResultCallback resultCallback;

  @BeforeEach
  void setup() {
    // not sure how much this tests beyond duplicating the code

    // to get info on container
    lenient()
        .when(dockerClient.inspectContainerCmd("dummy").exec().getState().getRunning())
        .thenReturn(true);

    // to get state of a container
    lenient().when(containerInfo.getState().getRunning()).thenReturn(true);

    // to run pull image
    lenient().when(dockerClient.pullImageCmd("dummy/image")).thenReturn(pullImageCmd);
    lenient()
        .when(pullImageCmd.exec(any(PullImageResultCallback.class)))
        .thenReturn(resultCallback);
  }

  @Test
  public void testDeployDocker() {
    ProfileConfig profileConfig =
        ProfileConfig.create("dummy", "dummy/image", "localhost", 6133, Set.of(), Map.of());
    DockerService armadilloDockerService = new DockerService(dockerClient, profileService);
    armadilloDockerService.startProfile(profileConfig.getName());
    assertEquals(RUNNING, armadilloDockerService.getProfileStatus(profileConfig.getName()));
    armadilloDockerService.removeProfile(profileConfig.getName());
  }
}
