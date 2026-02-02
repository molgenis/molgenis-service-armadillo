package org.molgenis.armadillo.profile;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.molgenis.armadillo.metadata.ProfileStatus.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.command.RemoveImageCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.molgenis.armadillo.exceptions.ImagePullFailedException;
import org.molgenis.armadillo.exceptions.ImageRemoveFailedException;
import org.molgenis.armadillo.exceptions.MissingImageException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.molgenis.armadillo.model.DockerImageInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DockerServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DockerClient dockerClient;

  @Mock private ProfileService profileService;
  private DockerService dockerService;

  @Mock private ProfileStatusService profileStatusService;

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
    dockerService = new DockerService(dockerClient, profileService, profileStatusService);

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

    // Mock version retrieval
    when(dockerService.getOpenContainersImageVersion("sha256:abcd")).thenReturn("v1.0.0");

    // Mock image size retrieval
    when(dockerService.getImageSize("sha256:abcd")).thenReturn(123_456_789L);

    // Mock image creation date retrieval
    when(dockerService.getImageCreationDate("sha256:abcd")).thenReturn("2025-08-05T12:34:56Z");

    dockerService.startProfile("default");

    // Verify Docker operations
    verify(dockerClient).pullImageCmd(profileConfig.getImage());
    verify(dockerClient).stopContainerCmd("default");
    verify(dockerClient).removeContainerCmd("default");
    verify(dockerClient).createContainerCmd(profileConfig.getImage());
    verify(dockerClient).startContainerCmd("default");

    verify(profileService)
        .updateImageMetaData(
            eq("default"),
            eq("sha256:abcd"),
            eq("v1.0.0"),
            eq(123_456_789L),
            eq("2025-08-05T12:34:56Z"),
            anyString(), // installDate generated dynamically in method
            anyBoolean());
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

    // Mock version retrieval
    when(dockerService.getOpenContainersImageVersion("sha256:new")).thenReturn("v1.0.0");

    // Mock image size retrieval
    when(dockerService.getImageSize("sha256:new")).thenReturn(987_654_321L);

    // Mock creation date retrieval
    when(dockerService.getImageCreationDate("sha256:new")).thenReturn("2025-08-05T12:34:56Z");

    // Return tags — optional
    when(dockerClient.inspectImageCmd("sha256:old").exec().getRepoTags()).thenReturn(List.of());

    // No containers use the old image
    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of());

    // Image removal by image ID
    var rmCmd = mock(RemoveImageCmd.class);
    when(dockerClient.removeImageCmd("sha256:old")).thenReturn(rmCmd);
    when(rmCmd.withForce(true)).thenReturn(rmCmd);
    doNothing().when(rmCmd).exec();

    // Act
    dockerService.startProfile("default");

    // Assert
    verify(dockerClient).removeImageCmd("sha256:old");
    verify(rmCmd).withForce(true);
    verify(rmCmd).exec();

    //
    verify(profileService)
        .updateImageMetaData(
            eq("default"),
            eq("sha256:new"),
            eq("v1.0.0"),
            eq(987_654_321L),
            eq("2025-08-05T12:34:56Z"),
            anyString(), // installDate dynamically generated
            anyBoolean());
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

    // Mock version retrieval
    when(dockerService.getOpenContainersImageVersion("sha256:same")).thenReturn("v1.0.0");

    // Mock image size retrieval
    when(dockerService.getImageSize("sha256:same")).thenReturn(555_000_000L);

    // Mock creation date retrieval (fixed the ID to "same" instead of "new")
    when(dockerService.getImageCreationDate("sha256:same")).thenReturn("2025-08-05T12:34:56Z");

    // Call the method under test
    assertDoesNotThrow(() -> dockerService.startProfile("default"));

    // Verify no image removal called
    verify(dockerClient, never()).removeImageCmd(anyString());

    // Verify metadata update includes image size and a null install date (no change in image ID)
    verify(profileService)
        .updateImageMetaData(
            eq("default"),
            eq("sha256:same"),
            eq("v1.0.0"),
            eq(555_000_000L),
            eq("2025-08-05T12:34:56Z"),
            isNull(), // installDate should be null since image ID did not change
            eq(false));
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
            emptyMap(),
            null,
            null,
            null,
            null,
            null,
            false);
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
    var spyService = spy(new DockerService(dockerClient, profileService, profileStatusService));
    doNothing().when(spyService).removeProfile(profileName);
    doNothing().when(spyService).removeImageIfUnused(imageId);

    // execute
    spyService.deleteProfile(profileName);

    // verify interactions
    verify(spyService).removeProfile(profileName);
    verify(profileService).getByName(profileName);
    verify(spyService).removeImageIfUnused(imageId);
  }

  @Test
  void deleteProfile_doesntThrowError() {
    var profileName = "default";
    var imageId = "sha256:test";

    // mock config with image ID
    var config = mock(ProfileConfig.class);
    when(config.getLastImageId()).thenReturn(imageId);
    when(profileService.getByName(profileName)).thenReturn(config);
    when(dockerClient.inspectImageCmd(imageId)).thenThrow(new NotFoundException(""));

    // spy DockerService to verify internal method calls
    var spyService = spy(new DockerService(dockerClient, profileService, profileStatusService));
    doNothing().when(spyService).removeProfile(profileName);
    doThrow(ImageRemoveFailedException.class).when(spyService).removeImageIfUnused(imageId);

    // execute
    assertDoesNotThrow(() -> spyService.deleteProfile(profileName));
  }

  @Test
  void testGetDockerImages() {
    // Mock Docker images
    Image image1 = mock(Image.class);
    Image image2 = mock(Image.class);

    when(image1.getId()).thenReturn("sha256:1111");
    when(image1.getRepoTags()).thenReturn(new String[] {"image1:latest"});

    when(image2.getId()).thenReturn("sha256:2222");
    when(image2.getRepoTags()).thenReturn(new String[] {"image2:dev"});

    var listImagesCmd = mock(com.github.dockerjava.api.command.ListImagesCmd.class);
    when(dockerClient.listImagesCmd()).thenReturn(listImagesCmd);
    when(listImagesCmd.withShowAll(true)).thenReturn(listImagesCmd);
    when(listImagesCmd.exec()).thenReturn(List.of(image1, image2));

    // Act
    List<DockerImageInfo> images = dockerService.getDockerImages();

    // Assert
    assertEquals(2, images.size());

    DockerImageInfo info1 = images.get(0);
    assertEquals("sha256:1111", info1.getImageId());
    assertArrayEquals(new String[] {"image1:latest"}, info1.getRepoTags());

    DockerImageInfo info2 = images.get(1);
    assertEquals("sha256:2222", info2.getImageId());
    assertArrayEquals(new String[] {"image2:dev"}, info2.getRepoTags());

    verify(dockerClient).listImagesCmd();
    verify(listImagesCmd).withShowAll(true);
    verify(listImagesCmd).exec();
  }

  @Test
  void updateImageMetaData_setsInstallDateWhenNewImage() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    ContainerConfig cfg = mock(ContainerConfig.class);

    when(dockerClient.inspectImageCmd("newImage")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(42L);
    when(resp.getConfig()).thenReturn(cfg);
    when(cfg.getLabels())
        .thenReturn(
            Map.of(
                "org.opencontainers.image.version", "1.0",
                "org.opencontainers.image.created", "2025-01-01T00:00:00Z"));

    dockerService.updateImageMetaData("profile1", null, "newImage", false);

    verify(profileService)
        .updateImageMetaData(
            eq("profile1"),
            eq("newImage"),
            eq("1.0"),
            eq(42L),
            eq("2025-01-01T00:00:00Z"),
            anyString(), // dynamically generated installDate
            anyBoolean());
  }

  @Test
  void updateImageMetaData_setsNullInstallDateWhenSameImage() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    ContainerConfig cfg = mock(ContainerConfig.class);

    when(dockerClient.inspectImageCmd("sameImage")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(123L);
    when(resp.getConfig()).thenReturn(cfg);
    when(cfg.getLabels())
        .thenReturn(
            Map.of(
                "org.opencontainers.image.version", "2.0",
                "org.opencontainers.image.created", "2025-02-02T00:00:00Z"));

    dockerService.updateImageMetaData("profile2", "sameImage", "sameImage", false);

    verify(profileService)
        .updateImageMetaData(
            eq("profile2"),
            eq("sameImage"),
            eq("2.0"),
            eq(123L),
            eq("2025-02-02T00:00:00Z"),
            isNull(), // no installDate when image ID unchanged
            anyBoolean());
  }

  @Test
  void getImageCreationDate_returnsLabelValue() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    ContainerConfig cfg = mock(ContainerConfig.class);

    when(dockerClient.inspectImageCmd("img")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getConfig()).thenReturn(cfg);
    when(cfg.getLabels()).thenReturn(Map.of("org.opencontainers.image.created", "2025-03-03"));

    assertEquals("2025-03-03", dockerService.getImageCreationDate("img"));
  }

  @Test
  void getImageCreationDate_returnsNullOnException() {
    when(dockerClient.inspectImageCmd("bad")).thenThrow(new RuntimeException("fail"));
    assertNull(dockerService.getImageCreationDate("bad"));
  }

  @Test
  void getImageSize_returnsSize() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);

    when(dockerClient.inspectImageCmd("img")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(9876L);

    assertEquals(9876L, dockerService.getImageSize("img"));
  }

  @Test
  void getImageSize_returnsNullOnException() {
    when(dockerClient.inspectImageCmd("oops")).thenThrow(new RuntimeException("fail"));
    assertNull(dockerService.getImageSize("oops"));
  }

  @Test
  void getOpenContainersImageVersion_returnsLabelValue() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    ContainerConfig cfg = mock(ContainerConfig.class);

    when(dockerClient.inspectImageCmd("img")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getConfig()).thenReturn(cfg);
    when(cfg.getLabels()).thenReturn(Map.of("org.opencontainers.image.version", "vX.Y.Z"));

    assertEquals("vX.Y.Z", dockerService.getOpenContainersImageVersion("img"));
  }

  @Test
  void getOpenContainersImageVersion_returnsUnknownWhenMissingLabel() {
    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    ContainerConfig cfg = mock(ContainerConfig.class);

    when(dockerClient.inspectImageCmd("img")).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getConfig()).thenReturn(cfg);
    when(cfg.getLabels()).thenReturn(Map.of()); // no version label

    assertEquals("Unknown Version", dockerService.getOpenContainersImageVersion("img"));
  }

  @Test
  void getOpenContainersImageVersion_returnsNullOnException() {
    when(dockerClient.inspectImageCmd("oops")).thenThrow(new RuntimeException("boom"));
    assertNull(dockerService.getOpenContainersImageVersion("oops"));
  }

  @Test
  void pullImage_emitsProgressUpdates_toProfileStatusService() {
    // Arrange profile returned by service
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn("repo/image:tag");
    when(profileService.getByName("default")).thenReturn(profile);

    // Stub pullImageCmd + capture the callback
    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);

    ArgumentCaptor<PullImageResultCallback> cbCap =
        ArgumentCaptor.forClass(PullImageResultCallback.class);

    when(pullImageCmd.exec(cbCap.capture())).thenReturn(new NonBlockingCallback());

    // Act: triggers pullImage() internally
    assertDoesNotThrow(() -> dockerService.startProfile("default"));

    // Get the captured callback
    PullImageResultCallback cb = cbCap.getValue();

    // Drive onNext events
    PullResponseItem it1 = mock(PullResponseItem.class);
    when(it1.getId()).thenReturn("layer1");
    when(it1.getStatus()).thenReturn("Downloading");
    cb.onNext(it1);
    verify(profileStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing profile"), eq(0), eq(1));

    PullResponseItem it2 = mock(PullResponseItem.class);
    when(it2.getId()).thenReturn("layer1");
    when(it2.getStatus()).thenReturn("Pull complete");
    cb.onNext(it2);
    verify(profileStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing profile"), eq(1), eq(1));

    PullResponseItem it3 = mock(PullResponseItem.class);
    when(it3.getId()).thenReturn("layer2");
    when(it3.getStatus()).thenReturn("Already exists");
    cb.onNext(it3);
    verify(profileStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing profile"), eq(2), eq(2));
  }

  @Test
  void pullImage_ignoresItemsWithoutId() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn("repo/image:tag");
    when(profileService.getByName("default")).thenReturn(profile);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);
    ArgumentCaptor<PullImageResultCallback> cbCap =
        ArgumentCaptor.forClass(PullImageResultCallback.class);
    when(pullImageCmd.exec(cbCap.capture())).thenAnswer(inv -> new NonBlockingCallback());

    assertDoesNotThrow(() -> dockerService.startProfile("default"));

    PullImageResultCallback cb = cbCap.getValue();

    // mock PullResponseItem instead of creating a real one
    PullResponseItem noId = mock(PullResponseItem.class);
    when(noId.getId()).thenReturn(null);
    when(noId.getStatus()).thenReturn("Downloading");

    cb.onNext(noId);

    // since id == null, it should skip calling updateStatus
    verify(profileStatusService, never())
        .updateStatus(eq("donkey"), eq("Installing profile"), any(), any());
  }

  @Test
  void pullImage_throwsMissingImage_whenConfigImageNull() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn(null);
    when(profileService.getByName("default")).thenReturn(profile);

    assertThrows(MissingImageException.class, () -> dockerService.startProfile("default"));
  }

  @Test
  void pullImage_mapsNotFound_toImagePullFailed() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn("repo/image:tag");
    when(profileService.getByName("default")).thenReturn(profile);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);
    // make exec() throw NotFound so pullImage catches and maps
    when(pullImageCmd.exec(any())).thenThrow(new NotFoundException("nope"));

    assertThrows(ImagePullFailedException.class, () -> dockerService.startProfile("default"));
  }

  @Test
  void pullImage_runtimeException_isSwallowedAndDoesNotThrow() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn("repo/image:tag");
    when(profileService.getByName("default")).thenReturn(profile);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);
    when(pullImageCmd.exec(any())).thenThrow(new RuntimeException("network down"));

    // per code, runtime is logged and tolerated
    assertDoesNotThrow(() -> dockerService.startProfile("default"));
  }

  @Test
  void pullImage_interruptedException_setsInterruptFlag_andThrows() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("donkey");
    when(profile.getImage()).thenReturn("repo/image:tag");
    when(profileService.getByName("default")).thenReturn(profile);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);

    // return a callback whose awaitCompletion(..) throws InterruptedException
    when(pullImageCmd.exec(any()))
        .thenReturn(
            new PullImageResultCallback() {
              @Override
              public PullImageResultCallback awaitCompletion() throws InterruptedException {
                throw new InterruptedException("boom");
              }

              @Override
              public boolean awaitCompletion(long t, TimeUnit u) throws InterruptedException {
                throw new InterruptedException("boom");
              }
            });

    assertThrows(ImagePullFailedException.class, () -> dockerService.startProfile("default"));
    // optional: assert interrupted flag is set on current thread
    assertTrue(Thread.currentThread().isInterrupted(), "thread interrupt flag should be set");
  }
}
