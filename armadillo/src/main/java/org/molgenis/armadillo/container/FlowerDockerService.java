package org.molgenis.armadillo.container;

import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.molgenis.armadillo.exceptions.ContainerNotFoundException;
import org.molgenis.armadillo.exceptions.DataPushFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class FlowerDockerService {

  private final DockerClient dockerClient;

  public FlowerDockerService(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public void copyDataToContainer(
      String containerName, String destDir, String fileName, InputStream data) {
    try {
      ensureDirectoryExists(containerName, destDir);
      byte[] bytes = data.readAllBytes();
      InputStream tarStream = createTarArchive(fileName, bytes);
      dockerClient
          .copyArchiveToContainerCmd(containerName)
          .withTarInputStream(tarStream)
          .withRemotePath(destDir)
          .exec();
    } catch (NotFoundException e) {
      throw new ContainerNotFoundException(containerName, e);
    } catch (DockerException e) {
      throw new DataPushFailedException(containerName, e);
    } catch (IOException e) {
      throw new DataPushFailedException(containerName, e);
    }
  }

  private void ensureDirectoryExists(String containerName, String dir) throws IOException {
    ExecCreateCmdResponse exec =
        dockerClient
            .execCreateCmd(containerName)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("mkdir", "-p", dir)
            .exec();
    try {
      dockerClient
          .execStartCmd(exec.getId())
          .exec(new ResultCallback.Adapter<Frame>())
          .awaitCompletion();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating directory " + dir, e);
    }
  }

  static InputStream createTarArchive(String fileName, byte[] content) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(baos)) {
      TarArchiveEntry entry = new TarArchiveEntry(fileName);
      entry.setSize(content.length);
      tar.putArchiveEntry(entry);
      tar.write(content);
      tar.closeArchiveEntry();
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }
}
