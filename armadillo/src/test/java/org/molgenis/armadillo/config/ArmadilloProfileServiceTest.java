package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.molgenis.armadillo.metadata.ProfileDetails.Status.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileDetails;

@ExtendWith(MockitoExtension.class)
public class ArmadilloProfileServiceTest {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  InspectContainerResponse containerInfo;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DockerClient dockerClient;

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
  public void testDeployDocker() throws InterruptedException {
    ProfileDetails profileDetails =
        ProfileDetails.create("dummy", "dummy/image", 6133, List.of(), null, null);
    ArmadilloProfileService armadilloProfileService = new ArmadilloProfileService(dockerClient);
    armadilloProfileService.startProfile(profileDetails);
    assertEquals(RUNNING, armadilloProfileService.getProfileStatus(profileDetails));
    armadilloProfileService.removeProfile(profileDetails.getName());
  }

  //  @Test
  //  @IfProfileValue(name = "spring.profiles.active", value = "integration-test")
  //  public void forReal() {
  //    ProfileDetails profileDetails =
  //        ProfileDetails.create(
  //            "exposome", "datashield/armadillo-rserver:6.2.0", 6133, List.of("dsBase"), null,
  // null);
  //    ArmadilloProfileService armadilloProfileService =
  //        new ArmadilloProfileService(
  //
  // DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder())
  //                .build());
  //    armadilloProfileService.startProfile(profileDetails);
  //    assertEquals(RUNNING, armadilloProfileService.getProfileStatus(profileDetails));
  //    armadilloProfileService.removeProfile(profileDetails.getName());
  //  }
}
