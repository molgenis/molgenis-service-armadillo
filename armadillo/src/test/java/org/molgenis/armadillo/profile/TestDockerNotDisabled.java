package org.molgenis.armadillo.profile;

import com.github.dockerjava.api.DockerClient;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileConfig;

@ExtendWith(MockitoExtension.class)
public class TestDockerNotDisabled {

  @Mock DockerClient dockerClient;
  DockerService dockerService = new DockerService(dockerClient);

  @Test
  public void testDockerNotDisabled() throws InterruptedException {
    // nothing should happen
    ProfileConfig dummyConfig =
        ProfileConfig.create("test", "test", "localhost", 6111, Set.of(), Map.of(), null);
    dockerService.getProfileStatus(dummyConfig);
    dockerService.startProfile(dummyConfig);
    dockerService.removeProfile(dummyConfig.getName());
  }
}
