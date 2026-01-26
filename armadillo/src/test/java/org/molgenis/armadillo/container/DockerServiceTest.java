package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.molgenis.armadillo.metadata.ContainerStatus.RUNNING;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import jakarta.ws.rs.ProcessingException;
import java.net.SocketException;
import java.util.List;
import java.util.Map;
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
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.armadillo.metadata.ContainerStatus;
import org.molgenis.armadillo.model.DockerImageInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DockerServiceTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  DockerClient dockerClient;

  @Mock private ContainerService containerService;
  private DockerService dockerService;

  @Mock private ContainerStatusService containerStatusService;

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
    dockerService = new DockerService(dockerClient, containerService, containerStatusService);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    lenient().when(dockerClient.pullImageCmd(anyString())).thenReturn(pullImageCmd);

    lenient().when(pullImageCmd.exec(any())).thenReturn(new NonBlockingCallback());
  }

  @Test
  void testGetContainerStatus() {
    String name = "default";
    var tags = List.of("2.0.0", "latest");
    var config = mock(ContainerConfig.class);
    when(containerService.getByName(name)).thenReturn(config);

    var containerState = mock(ContainerState.class);
    when(containerState.getRunning()).thenReturn(true);
    var inspectContainerResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd(name).exec()).thenReturn(inspectContainerResponse);
    when(inspectContainerResponse.getState()).thenReturn(containerState);
    when(dockerClient.inspectImageCmd(name).exec().getRepoTags()).thenReturn(tags);
    when(inspectContainerResponse.getName()).thenReturn(name);

    var expected = ContainerInfo.create(tags, RUNNING);
    var containerInfo = dockerService.getContainerStatus(name);

    assertEquals(expected, containerInfo);
    verify(containerService).getByName(name);
  }

  @Test
  void testGetContainerStatusNotFound() {
    var config = mock(ContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(config);
    when(dockerClient.inspectContainerCmd("default").exec()).thenThrow(new NotFoundException(""));

    var expected = ContainerInfo.create(ContainerStatus.NOT_FOUND);

    var containerInfo = dockerService.getContainerStatus("default");

    assertEquals(expected, containerInfo);
    verify(containerService).getByName("default");
  }

  @Test
  void testGetContainerStatusDockerOffline() {
    var config = mock(ContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(config);
    when(dockerClient.inspectContainerCmd("default").exec())
        .thenThrow(new ProcessingException(new SocketException()));

    var expected = ContainerInfo.create(ContainerStatus.DOCKER_OFFLINE);

    var containerInfo = dockerService.getContainerStatus("default");

    assertEquals(expected, containerInfo);
    verify(containerService).getByName("default");
  }

  @Test
  void testGetAllContainerStatuses() {
    when(containerService.getAll()).thenReturn(createExampleSettings());
    var tags = List.of("2.0.0", "latest");
    var names = List.of("platform-1", "platform-2");
    var containerDefault = mock(Container.class);
    when(containerDefault.getNames()).thenReturn(new String[] {"/platform-1"});
    when(containerDefault.getImageId()).thenReturn("platform-1");
    when(containerDefault.getState()).thenReturn("running");
    when(dockerClient.inspectImageCmd("platform-1").exec().getRepoTags()).thenReturn(tags);

    var containers = List.of(containerDefault);
    when(dockerClient.listContainersCmd().withShowAll(true).withNameFilter(names).exec())
        .thenReturn(containers);

    var expected =
        Map.of(
            "platform-1",
            ContainerInfo.create(tags, RUNNING),
            "platform-2",
            ContainerInfo.create(ContainerStatus.NOT_FOUND));

    var result = dockerService.getAllContainerStatuses();

    assertEquals(expected, result);
  }

  @Test
  void testStartContainerNoImage() {
    var config = mock(ContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(config);
    when(config.getImage()).thenReturn(null);

    assertThrows(
        MissingImageException.class, () -> dockerService.pullImageStartContainer("default"));
  }

  @Test
  void testInstallImageNull() {
    var config = mock(ContainerConfig.class);
    when(config.getImage()).thenReturn(null);
    assertThrows(MissingImageException.class, () -> dockerService.installImage(config));
  }

  @Test
  void testInstallImage() {
    var config = mock(ContainerConfig.class);
    String image = "datashield/rock-something-something:latest";
    when(config.getImage()).thenReturn(image);
    when(config.getPort()).thenReturn(6311);

    assertDoesNotThrow(() -> dockerService.installImage(config));
    verify(dockerClient).createContainerCmd(image);
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  void testStartContainer() {
    var config =
        DefaultContainerConfig.create(
            "default", "image:tag", "localhost", 6311, null, null, null, List.of(), Map.of());
    when(containerService.getByName("default")).thenReturn(config);

    var inspectResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(inspectResponse);
    when(inspectResponse.getImageId()).thenReturn("sha256:abcd");

    when(dockerService.getImageSize("sha256:abcd")).thenReturn(123_456_789L);

    dockerService.pullImageStartContainer("default");

    verify(dockerClient).pullImageCmd(config.getImage());
    verify(dockerClient).stopContainerCmd("default");
    verify(dockerClient).removeContainerCmd("default");
    verify(dockerClient).createContainerCmd(config.getImage());
    verify(dockerClient).startContainerCmd("default");

    verify(containerService)
        .updateImageMetaData(
            eq("default"),
            eq("sha256:abcd"),
            eq("Unknown Version"),
            eq(123456789L),
            isNull(),
            anyString());
  }

  @Test
  void testStartImageRemovalWhenIdChanges() {
    var config = mock(ContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(config);
    when(config.getName()).thenReturn("default");
    when(config.getImage()).thenReturn("some/image");
    when(config.getLastImageId()).thenReturn("sha256:old");

    var containerInfo = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(containerInfo);
    when(containerInfo.getImageId()).thenReturn("sha256:new");

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of());

    var rmCmd = mock(RemoveImageCmd.class);
    when(dockerClient.removeImageCmd("sha256:old")).thenReturn(rmCmd);
    when(rmCmd.withForce(true)).thenReturn(rmCmd);

    dockerService.pullImageStartContainer("default");

    verify(dockerClient).removeImageCmd("sha256:old");
    verify(rmCmd).withForce(true);
    verify(rmCmd).exec();

    verify(containerService)
        .updateImageMetaData(
            eq("default"), // [0] Name
            eq("sha256:new"), // [1] New ID
            eq("Unknown Version"), // [2] Match the ACTUAL fallback string
            eq(0L), // [3] Match the ACTUAL 0L (from unmocked size)
            isNull(), // [4] Creation date
            anyString() // [5] New install date (timestamp)
            );
  }

  @Test
  void testStartImageNotRemovedWhenIdUnchanged() {
    var config = mock(ContainerConfig.class);
    when(containerService.getByName("default")).thenReturn(config);
    when(config.getImage()).thenReturn("some/image");
    when(config.getLastImageId()).thenReturn("sha256:same");

    var inspectResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd("default").exec()).thenReturn(inspectResponse);
    when(inspectResponse.getImageId()).thenReturn("sha256:same");

    var inspectImageResponse = mock(InspectImageResponse.class, RETURNS_DEEP_STUBS);
    when(dockerClient.inspectImageCmd("sha256:same").exec()).thenReturn(inspectImageResponse);
    when(inspectImageResponse.getSize()).thenReturn(123456789L);

    when(dockerClient.pullImageCmd(any())).thenReturn(mock(PullImageCmd.class, RETURNS_DEEP_STUBS));
    when(dockerClient.stopContainerCmd(any())).thenReturn(mock(StopContainerCmd.class));
    when(dockerClient.removeContainerCmd(any())).thenReturn(mock(RemoveContainerCmd.class));
    when(dockerClient.createContainerCmd(any()))
        .thenReturn(mock(CreateContainerCmd.class, RETURNS_DEEP_STUBS));
    when(dockerClient.startContainerCmd(any())).thenReturn(mock(StartContainerCmd.class));

    assertDoesNotThrow(() -> dockerService.pullImageStartContainer("default"));

    verify(dockerClient, never()).removeImageCmd(anyString());

    verify(containerService)
        .updateImageMetaData(
            eq("default"), // Pos 0: Name
            eq("sha256:same"), // Pos 1: Match the ACTUAL "sha256:same"
            eq("Unknown Version"), // Pos 2: Version
            eq(123456789L), // Pos 3: Match the ACTUAL 123456789L from your mock
            isNull(), // Pos 4: Creation date
            isNull() // Pos 5: Match the ACTUAL null (since ID didn't change)
            );
  }

  private List<ContainerConfig> createExampleSettings() {
    ContainerConfig container1 =
        DefaultContainerConfig.create(
            "platform-1", "image:tag", "localhost", 6311, null, null, null, List.of(), Map.of());

    ContainerConfig container2 =
        DefaultContainerConfig.create(
            "platform-2", "image:tag", "localhost", 6311, null, null, null, List.of(), Map.of());

    return List.of(container1, container2);
  }

  @Test
  void removeImageIfUnused_skipsWhenImageIdIsNull() {
    assertDoesNotThrow(() -> dockerService.deleteImageIfUnused(null));

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
        ImageRemoveFailedException.class, () -> dockerService.deleteImageIfUnused("sha256:inuse"));
  }

  @Test
  void removeImageIfUnused_throwsErrorWhenNoImage() {
    var container = mock(Container.class);
    when(container.getImageId()).thenThrow(NotFoundException.class);

    var listCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listCmd);
    when(listCmd.exec()).thenReturn(List.of(container));

    assertThrows(
        ImageRemoveFailedException.class, () -> dockerService.deleteImageIfUnused("sha256:inuse"));
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

    dockerService.deleteImageIfUnused(imageId);

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

    assertDoesNotThrow(() -> dockerService.deleteImageIfUnused(imageId));
  }

  @Test
  void deleteContainer_removesContainerAndImage() {
    var containerName = "default";
    var imageId = "sha256:test";

    var config = mock(ContainerConfig.class);
    when(config.getLastImageId()).thenReturn(imageId);
    when(containerService.getByName(containerName)).thenReturn(config);

    var spyService = spy(new DockerService(dockerClient, containerService, containerStatusService));
    doNothing().when(spyService).stopAndRemoveContainer(containerName);
    doNothing().when(spyService).deleteImageIfUnused(imageId);

    spyService.removeContainerDeleteImage(containerName);
    verify(spyService).stopAndRemoveContainer(containerName);
    verify(containerService).getByName(containerName);
    verify(spyService).deleteImageIfUnused(imageId);
  }

  @Test
  void deleteContainer_doesntThrowError() {
    var containerName = "default";
    var imageId = "sha256:test";

    var config = mock(ContainerConfig.class);
    when(config.getLastImageId()).thenReturn(imageId);
    when(containerService.getByName(containerName)).thenReturn(config);

    var spyService = spy(new DockerService(dockerClient, containerService, containerStatusService));

    doNothing().when(spyService).stopAndRemoveContainer(containerName);

    doThrow(
            new ImageRemoveFailedException(
                imageId, "Image is currently in use by another container"))
        .when(spyService)
        .deleteImageIfUnused(imageId);

    assertDoesNotThrow(() -> spyService.removeContainerDeleteImage(containerName));

    verify(spyService).stopAndRemoveContainer(containerName);
    verify(spyService).deleteImageIfUnused(imageId);
  }

  @Test
  void testGetDockerImages() {
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

    List<DockerImageInfo> images = dockerService.getDockerImages();

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
    String containerName = "generic-container";
    String newImageId = "sha256:new-id";

    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);

    when(dockerClient.inspectImageCmd(newImageId)).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(500_000_000L);

    dockerService.updateImageMetaData(containerName, null, newImageId);

    verify(containerService)
        .updateImageMetaData(
            eq(containerName),
            eq(newImageId),
            isNull(), // No OCI version
            eq(500_000_000L),
            isNull(), // No OCI creation date
            anyString() // The generated installDate (Instant.now())
            );
  }

  @Test
  void updateImageMetaData_setsNullInstallDateWhenSameImage() {
    String containerName = "generic-container";
    String imageId = "sha256:same-id";

    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);

    when(dockerClient.inspectImageCmd(imageId)).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(123L);

    dockerService.updateImageMetaData(containerName, imageId, imageId);

    verify(containerService)
        .updateImageMetaData(
            eq(containerName),
            eq(imageId),
            isNull(), // No version (labels removed)
            eq(123L),
            isNull(), // No creation date (labels removed)
            isNull() // CRITICAL: installDate must be null when image ID is unchanged
            );
  }

  @Test
  void updateImageMetaData_setsOciDataForDatashield() {
    String containerName = "datashield-1";
    String imageId = "sha256:ds-image";

    var config = mock(DatashieldContainerConfig.class);
    when(containerService.getByName(containerName)).thenReturn(config);

    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);
    com.github.dockerjava.api.model.ContainerConfig dockerCfg =
        mock(com.github.dockerjava.api.model.ContainerConfig.class);

    when(dockerClient.inspectImageCmd(imageId)).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(1024L);
    when(resp.getConfig()).thenReturn(dockerCfg);
    when(dockerCfg.getLabels())
        .thenReturn(
            Map.of(
                "org.opencontainers.image.version", "3.0.0",
                "org.opencontainers.image.created", "2025-05-05T12:00:00Z"));

    dockerService.updateImageMetaData(containerName, null, imageId);

    verify(containerService)
        .updateImageMetaData(
            eq(containerName),
            eq(imageId),
            eq("3.0.0"), // OCI Version
            eq(1024L), // Size
            eq("2025-05-05T12:00:00Z"), // OCI Created
            anyString() // Generated InstallDate
            );
  }

  @Test
  void updateImageMetaData_skipsOciDataForDefaultConfig() {
    String containerName = "basic-container";
    String imageId = "sha256:basic-image";

    var config =
        DefaultContainerConfig.create(
            "default", "image:tag", "localhost", 6311, null, null, null, List.of(), Map.of());

    when(containerService.getByName(containerName)).thenReturn(config);

    InspectImageCmd cmd = mock(InspectImageCmd.class);
    InspectImageResponse resp = mock(InspectImageResponse.class);

    when(dockerClient.inspectImageCmd(imageId)).thenReturn(cmd);
    when(cmd.exec()).thenReturn(resp);
    when(resp.getSize()).thenReturn(500L);

    dockerService.updateImageMetaData(containerName, null, imageId);

    verify(containerService)
        .updateImageMetaData(
            eq(containerName),
            eq(imageId),
            isNull(), // No OCI Version support
            eq(500L),
            isNull(), // No OCI Created support
            anyString());
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
    com.github.dockerjava.api.model.ContainerConfig cfg =
        mock(com.github.dockerjava.api.model.ContainerConfig.class);

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
    com.github.dockerjava.api.model.ContainerConfig cfg =
        mock(com.github.dockerjava.api.model.ContainerConfig.class);

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
  void pullImage_emitsProgressUpdates_toContainerStatusService() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("donkey");
    when(container.getImage()).thenReturn("repo/image:tag");
    when(containerService.getByName("default")).thenReturn(container);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);

    ArgumentCaptor<PullImageResultCallback> cbCap =
        ArgumentCaptor.forClass(PullImageResultCallback.class);

    when(pullImageCmd.exec(cbCap.capture())).thenReturn(new NonBlockingCallback());

    assertDoesNotThrow(() -> dockerService.pullImageStartContainer("default"));

    PullImageResultCallback cb = cbCap.getValue();

    PullResponseItem it1 = mock(PullResponseItem.class);
    when(it1.getId()).thenReturn("layer1");
    when(it1.getStatus()).thenReturn("Downloading");
    cb.onNext(it1);
    verify(containerStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing container"), eq(0), eq(1));

    PullResponseItem it2 = mock(PullResponseItem.class);
    when(it2.getId()).thenReturn("layer1");
    when(it2.getStatus()).thenReturn("Pull complete");
    cb.onNext(it2);
    verify(containerStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing container"), eq(1), eq(1));

    PullResponseItem it3 = mock(PullResponseItem.class);
    when(it3.getId()).thenReturn("layer2");
    when(it3.getStatus()).thenReturn("Already exists");
    cb.onNext(it3);
    verify(containerStatusService, atLeastOnce())
        .updateStatus(eq("donkey"), eq("Installing container"), eq(2), eq(2));
  }

  @Test
  void pullImage_ignoresItemsWithoutId() {
    var containerName = "default";
    var displayName = "generic-container";
    var imageName = "repo/image:tag";

    var container = mock(ContainerConfig.class);
    when(container.getName()).thenReturn(displayName);
    when(container.getImage()).thenReturn(imageName);
    when(containerService.getByName(containerName)).thenReturn(container);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd(imageName)).thenReturn(pullImageCmd);

    ArgumentCaptor<PullImageResultCallback> cbCap =
        ArgumentCaptor.forClass(PullImageResultCallback.class);
    when(pullImageCmd.exec(cbCap.capture())).thenReturn(new NonBlockingCallback());

    assertDoesNotThrow(() -> dockerService.pullImageStartContainer(containerName));

    PullImageResultCallback cb = cbCap.getValue();
    PullResponseItem noIdItem = mock(PullResponseItem.class);
    when(noIdItem.getId()).thenReturn(null);
    when(noIdItem.getStatus()).thenReturn("Pulling from repository");

    cb.onNext(noIdItem);

    verify(containerStatusService, never())
        .updateStatus(anyString(), anyString(), anyInt(), anyInt());
  }

  @Test
  void pullImage_throwsMissingImage_whenConfigImageNull() {
    var containerName = "default";

    var container = mock(ContainerConfig.class);
    when(container.getName()).thenReturn("generic-container");
    when(container.getImage()).thenReturn(null); // The error trigger
    when(containerService.getByName(containerName)).thenReturn(container);

    assertThrows(
        MissingImageException.class, () -> dockerService.pullImageStartContainer(containerName));

    verify(dockerClient, never()).pullImageCmd(anyString());
  }

  @Test
  void pullImage_mapsNotFound_toImagePullFailed() {
    var containerName = "default";
    var imageName = "repo/image:tag";

    var container = mock(ContainerConfig.class);
    when(container.getName()).thenReturn("generic-container");
    when(container.getImage()).thenReturn(imageName);
    when(containerService.getByName(containerName)).thenReturn(container);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd(imageName)).thenReturn(pullImageCmd);

    when(pullImageCmd.exec(any())).thenThrow(new NotFoundException("Image not found in registry"));

    assertThrows(
        ImagePullFailedException.class, () -> dockerService.pullImageStartContainer(containerName));
  }

  @Test
  void pullImage_runtimeException_isSwallowedAndDoesNotThrow() {
    String containerName = "default";
    String imageName = "repo/image:tag";

    var container = mock(ContainerConfig.class);
    when(container.getName()).thenReturn("generic-container");
    when(container.getImage()).thenReturn(imageName);
    when(containerService.getByName(containerName)).thenReturn(container);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd(imageName)).thenReturn(pullImageCmd);

    when(pullImageCmd.exec(any())).thenThrow(new RuntimeException("unexpected network failure"));

    assertDoesNotThrow(() -> dockerService.pullImageStartContainer(containerName));
  }

  @Test
  void pullImage_interruptedException_setsInterruptFlag_andThrows() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("donkey");
    when(container.getImage()).thenReturn("repo/image:tag");
    when(containerService.getByName("default")).thenReturn(container);

    PullImageCmd pullImageCmd = mock(PullImageCmd.class);
    when(dockerClient.pullImageCmd("repo/image:tag")).thenReturn(pullImageCmd);

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

    assertThrows(
        ImagePullFailedException.class, () -> dockerService.pullImageStartContainer("default"));
    assertTrue(Thread.currentThread().isInterrupted(), "thread interrupt flag should be set");
  }
}
