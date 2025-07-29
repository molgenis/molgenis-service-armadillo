package org.molgenis.armadillo.profile;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.metadata.ProfileStatus.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.ImageRemoveFailedException;
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

  /** Test-only callback that never blocks. */
  private static class NonBlockingCallback extends PullImageResultCallback {
    @Override
    public PullImageResultCallback awaitCompletion() {
      return this; // no-op
    }

    @Override
    public boolean awaitCompletion(long timeout, TimeUnit unit) {
      return true; // immediate success
    }
  }

  @BeforeEach
  void setup() {
    dockerService = new DockerService(dockerClient, profileService);

    // lenient so tests that don't pull images won't fail strict-stubbing checks
    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    lenient().when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageCmd);

    // return a non-blocking callback from exec(..)
    lenient().when(pullImageCmd.exec(any())).thenReturn(new NonBlockingCallback());
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
    when(inspectContainerResponse.getState()).thenReturn(containerState);
    when(dockerClient.inspectImageCmd(name).exec().getRepoTags()).thenReturn(tags);
    when(inspectContainerResponse.getName()).thenReturn(name);

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

    // Stub inspectContainerCmd to return an image ID
    var inspectResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(inspectResponse);
    when(inspectResponse.getImageId()).thenReturn("sha256:abcd");

    dockerService.startProfile("default");

    verify(dockerClient).pullImageCmd(profileConfig.getImage());
    verify(dockerClient).stopContainerCmd("default");
    verify(dockerClient).removeContainerCmd("default");
    verify(dockerClient).createContainerCmd(profileConfig.getImage());
    verify(dockerClient).startContainerCmd("default");
    verify(profileService).updateLastImageId("default", "sha256:abcd");
  }

  @Test
  void testStartImageRemovalWhenIdChanges() {
    var profileCfg = mock(ProfileConfig.class);
    when(profileService.getByName("default")).thenReturn(profileCfg);
    when(profileCfg.getImage()).thenReturn("datashield/armadillo-rserver");
    when(profileCfg.getLastImageId()).thenReturn("sha256:old");

    var containerInfo = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(containerInfo);
    when(containerInfo.getImageId()).thenReturn("sha256:new");

    // return tags — optional now
    when(dockerClient.inspectImageCmd("sha256:old").exec().getRepoTags()).thenReturn(List.of());

    // no containers use the old image
    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of());

    // image-removal by image ID
    var rmCmd = mock(RemoveImageCmd.class);
    when(dockerClient.removeImageCmd("sha256:old")).thenReturn(rmCmd);
    when(rmCmd.withForce(true)).thenReturn(rmCmd);
    doNothing().when(rmCmd).exec();

    // act
    dockerService.startProfile("default");

    // assert
    verify(dockerClient).removeImageCmd("sha256:old");
    verify(rmCmd).withForce(true);
    verify(rmCmd).exec();
    verify(profileService).updateLastImageId("default", "sha256:new");
  }

  @Test
  void testStartImageNotRemovedWhenIdUnchanged() {
    var mockProfileConfig = mock(ProfileConfig.class);
    when(profileService.getByName("default")).thenReturn(mockProfileConfig);
    when(mockProfileConfig.getImage()).thenReturn("datashield/armadillo-rserver");
    when(mockProfileConfig.getLastImageId()).thenReturn("sha256:same");

    var inspectContainerResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(inspectContainerResponse);
    when(inspectContainerResponse.getImageId()).thenReturn("sha256:same");

    // Call the method under test
    assertDoesNotThrow(() -> dockerService.startProfile("default"));

    // Verify no image removal called
    verify(dockerClient, never()).removeImageCmd(anyString());

    // Verify last image ID update still happens (to same ID)
    verify(profileService).updateLastImageId("default", "sha256:same");
  }

  private List<ProfileConfig> createExampleSettings() {
    var profile1 = ProfileConfig.createDefault();
    var profile2 =
        ProfileConfig.create(
            "omics",
            "datashield/armadillo-rserver-omics",
            "localhost",
            6312,
            Set.of("dsBase", "dsOmics"),
            emptySet(),
            emptyMap(),
            null);
    return List.of(profile1, profile2);
  }

  @Test
  void removeImageIfUnused_skipsWhenImageIdIsNull() {
    assertDoesNotThrow(() -> dockerService.removeImageIfUnused(null));

    verify(dockerClient, never()).inspectImageCmd(anyString());
    verify(dockerClient, never()).removeImageCmd(anyString());
  }

  @Test
  void removeImageIfUnused_throwsErrorWhenInUse() {
    var container = mock(Container.class);
    when(container.getImageId()).thenReturn("sha256:inuse");

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of(container));

    assertThrows(
        ImageRemoveFailedException.class, () -> dockerService.removeImageIfUnused("sha256:inuse"));
  }

  @Test
  void removeImageIfUnused_throwsErrorWhenNoImage() {
    var container = mock(Container.class);
    when(container.getImageId()).thenThrow(NotFoundException.class);

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of(container));

    assertThrows(
        ImageRemoveFailedException.class, () -> dockerService.removeImageIfUnused("sha256:inuse"));
  }

  @Test
  void removeImageIfUnused_removesImageById() {
    String imageId = "sha256:unused";

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of()); // not in use

    var rmCmd = mock(RemoveImageCmd.class);
    when(dockerClient.removeImageCmd(imageId)).thenReturn(rmCmd);
    when(rmCmd.withForce(true)).thenReturn(rmCmd);
    doNothing().when(rmCmd).exec();

    dockerService.removeImageIfUnused(imageId);

    verify(dockerClient).removeImageCmd(imageId);
    verify(rmCmd).withForce(true);
    verify(rmCmd).exec();
  }

  @Test
  void removeImageIfUnused_handlesImageNotFound() {
    String imageId = "sha256:missing";

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of()); // not in use

    when(dockerClient.inspectImageCmd(imageId)).thenThrow(new NotFoundException(""));

    assertDoesNotThrow(() -> dockerService.removeImageIfUnused(imageId));
  }

  @Test
  void deleteProfile_removesProfileAndImage() {
    var profileName = "default";
    var imageId = "sha256:test";

    // mock config with image ID
    var config = mock(ProfileConfig.class);
    when(config.getLastImageId()).thenReturn(imageId);
    when(profileService.getByName(profileName)).thenReturn(config);

    // spy DockerService to verify internal method calls
    var spyService = spy(new DockerService(dockerClient, profileService));
    doNothing().when(spyService).removeProfile(profileName);
    doNothing().when(spyService).removeImageIfUnused(imageId);

    // execute
    spyService.deleteProfile(profileName);

    // verify interactions
    verify(spyService).removeProfile(profileName);
    verify(profileService).getByName(profileName);
    verify(spyService).removeImageIfUnused(imageId);
  }
}
