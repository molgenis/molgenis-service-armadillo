package org.molgenis.armadillo.profile;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.metadata.ProfileStatus.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.MissingImageException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;

@ExtendWith(MockitoExtension.class)
class DockerServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DockerClient dockerClient;

  @Mock private ProfileService profileService;
  private DockerService dockerService;

  @BeforeEach
  void setup() {
    dockerService = new DockerService(dockerClient, profileService);
  }

  @Test
  void testGetProfileStatus() {
    String imageId = "1234";
    String name = "default";
    var tags = List.of("2.0.0", "latest");
    var containerState = mock(ContainerState.class);
    when(containerState.getRunning()).thenReturn(true);
    var inspectContainerResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd(name).exec()).thenReturn(inspectContainerResponse);
    when(dockerClient.inspectImageCmd(name).exec().getRepoTags()).thenReturn(tags);
    when(inspectContainerResponse.getName()).thenReturn(name);
    when(inspectContainerResponse.getState()).thenReturn(containerState);

    var expected = ContainerInfo.create(tags, RUNNING);

    var containerInfo = dockerService.getProfileStatus("default");

    assertEquals(expected, containerInfo);
    verify(profileService).getByName("default");
  }

  @Test
  void testGetProfileStatusNotFound() {
    when(dockerClient.inspectContainerCmd("default").exec()).thenThrow(new NotFoundException(""));
    var expected = ContainerInfo.create(ProfileStatus.NOT_FOUND);

    var containerInfo = dockerService.getProfileStatus("default");

    assertEquals(expected, containerInfo);
    verify(profileService).getByName("default");
  }

  @Test
  void testGetProfileStatusDockerOffline() {
    when(dockerClient.inspectContainerCmd("default").exec())
        .thenThrow(new ProcessingException(new SocketException()));
    var expected = ContainerInfo.create(ProfileStatus.DOCKER_OFFLINE);

    var containerInfo = dockerService.getProfileStatus("default");

    assertEquals(expected, containerInfo);
    verify(profileService).getByName("default");
  }

  @Test
  void testGetAllProfileStatuses() {
    when(profileService.getAll()).thenReturn(createExampleSettings());
    var tags = List.of("2.0.0", "latest");
    var names = List.of("default", "omics");
    var containerDefault = mock(Container.class);
    when(containerDefault.getNames()).thenReturn(List.of("/default").toArray(String[]::new));
    when(containerDefault.getImageId()).thenReturn("default");
    when(containerDefault.getState()).thenReturn("running");
    when(dockerClient.inspectImageCmd("default").exec().getRepoTags()).thenReturn(tags);

    var containers = List.of(containerDefault);
    when(dockerClient.listContainersCmd().withShowAll(true).withNameFilter(names).exec())
        .thenReturn(containers);

    var expected =
        Map.of(
            "default",
            ContainerInfo.create(tags, RUNNING),
            "omics",
            ContainerInfo.create(ProfileStatus.NOT_FOUND));

    var result = dockerService.getAllProfileStatuses();

    assertEquals(expected, result);
  }

  @Test
  void testStartProfileNoImage() {
    var profileConfig = mock(ProfileConfig.class);
    when(profileService.getByName("default")).thenReturn(profileConfig);

    assertThrows(MissingImageException.class, () -> dockerService.startProfile("default"));
  }

  @Test
  void testInstallImageNull() {
    ProfileConfig profileConfig = mock(ProfileConfig.class);
    when(profileConfig.getImage()).thenReturn(null);
    assertThrows(MissingImageException.class, () -> dockerService.installImage(profileConfig));
  }

  @Test
  void testInstallImage() {
    ProfileConfig profileConfig = mock(ProfileConfig.class);
    String image = "datashield/rock-something-something:latest";
    when(profileConfig.getImage()).thenReturn(image);
    when(profileConfig.getPort()).thenReturn(6311);
    assertDoesNotThrow(() -> dockerService.installImage(profileConfig));
    verify(dockerClient).createContainerCmd(image);
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  void testStartProfile() {
    var profileConfig = ProfileConfig.createDefault();
    when(profileService.getByName("default")).thenReturn(profileConfig);

    dockerService.startProfile("default");

    verify(dockerClient).pullImageCmd(profileConfig.getImage());
    verify(dockerClient).stopContainerCmd("default");
    verify(dockerClient).removeContainerCmd("default");
    verify(dockerClient).createContainerCmd(profileConfig.getImage());
    verify(dockerClient).startContainerCmd("default");
  }

  private List<ProfileConfig> createExampleSettings() {
    var profile1 = ProfileConfig.createDefault();
    var profile2 =
        ProfileConfig.create(
            "omics",
            "datashield/armadillo-rserver-omics",
            false,
            null,
            "localhost",
            6312,
            Set.of("dsBase", "dsOmics"),
            emptySet(),
            emptyMap());
    return List.of(profile1, profile2);
  }
}
