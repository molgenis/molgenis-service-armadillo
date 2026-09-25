package org.molgenis.armadillo.container;

import static org.molgenis.armadillo.controller.ContainerDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.molgenis.armadillo.exceptions.ContainerNotFoundException;
import org.molgenis.armadillo.exceptions.DataPushFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class FlowerDockerService {

  private static final int PIPE_BUFFER_BYTES = 1024 * 1024;

  private final DockerClient dockerClient;
  private final DockerService dockerService;

  public FlowerDockerService(DockerClient dockerClient, DockerService dockerService) {
    this.dockerClient = dockerClient;
    this.dockerService = dockerService;
  }

  public void copyDataToContainer(
      String containerName, String destDir, String fileName, InputStream data, long size) {
    String dockerContainerName = dockerService.asContainerName(containerName);
    try {
      ensureDirectoryExists(dockerContainerName, destDir);
      streamTarToContainer(dockerContainerName, destDir, fileName, data, size);
    } catch (NotFoundException e) {
      throw new ContainerNotFoundException(containerName, e);
    } catch (DockerException | IOException e) {
      throw new DataPushFailedException(containerName, e);
    }
  }

  private void ensureDirectoryExists(String dockerContainerName, String dir) throws IOException {
    ExecCreateCmdResponse exec =
        dockerClient
            .execCreateCmd(dockerContainerName)
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
    Long exitCode = dockerClient.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
    if (exitCode == null || exitCode != 0) {
      throw new IOException("Failed to create directory " + dir + " (exit code " + exitCode + ")");
    }
  }

  // The tar is written on a separate thread into a pipe that Docker reads from, so memory use
  // stays at one pipe buffer regardless of file size.
  private void streamTarToContainer(
      String dockerContainerName, String destDir, String fileName, InputStream data, long size)
      throws IOException {
    PipedInputStream tarStream = new PipedInputStream(PIPE_BUFFER_BYTES);
    PipedOutputStream tarOut = new PipedOutputStream(tarStream);
    FutureTask<Void> writer =
        new FutureTask<>(
            () -> {
              try (tarOut) {
                writeTar(tarOut, fileName, data, size);
              }
              return null;
            });
    Thread.ofVirtual().start(writer);
    try (tarStream) {
      dockerClient
          .copyArchiveToContainerCmd(dockerContainerName)
          .withTarInputStream(tarStream)
          .withRemotePath(destDir)
          .exec();
    }
    awaitWriter(writer);
  }

  private static void awaitWriter(Future<Void> writer) throws IOException {
    try {
      writer.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while writing tar stream", e);
    } catch (ExecutionException e) {
      throw new IOException("Failed to write tar stream", e.getCause());
    }
  }

  // POSIX big-number headers: plain tar headers cannot describe entries of 8GB or more.
  static void writeTar(OutputStream out, String fileName, InputStream content, long size)
      throws IOException {
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(out)) {
      tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
      TarArchiveEntry entry = new TarArchiveEntry(fileName);
      entry.setSize(size);
      tar.putArchiveEntry(entry);
      content.transferTo(tar);
      tar.closeArchiveEntry();
    }
  }
}
