package org.molgenis.armadillo.profile;

import com.github.dockerjava.api.DockerClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;

@ExtendWith(MockitoExtension.class)
public class TestDockerNotDisabled {

  @Mock DockerClient dockerClient;
  @Mock ProfileService profileService;
  DockerService dockerService = new DockerService(dockerClient, profileService);

  @Test
  public void testDockerNotDisabled() {
    // nothing should happen
    ProfileConfig dummyConfig =
        ProfileConfig.create("test", "test", "localhost", 6111, Set.of(), Map.of(), null);
    dockerService.getProfileStatus(dummyConfig.getName());
    dockerService.startProfile(dummyConfig.getName());
    dockerService.removeProfile(dummyConfig.getName());
  }
}
