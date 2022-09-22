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
  ProfileService profileService = new ProfileService(dockerClient, false);

  @Test
  public void testDockerNotDisabled() throws InterruptedException {
    // nothing should happen
    ProfileConfig dummyConfig =
        ProfileConfig.create("test", "test", "localhost", 6111, Set.of(), Map.of(), null);
    profileService.getProfileStatus(dummyConfig);
    profileService.startProfile(dummyConfig);
    profileService.removeProfile(dummyConfig.getName());
  }
}
