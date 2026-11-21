package org.molgenis.connection.flower.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.concurrent.FutureTask;
import org.molgenis.connection.exceptions.ContainerNotFoundException;
import org.molgenis.connection.exceptions.DataPushFailedException;
import org.molgenis.connection.flower.helpers.TarFile;
import org.molgenis.connection.flower.helpers.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlowerDockerService {
  /** Helper class for FlowerContainerConnection */
  private final DockerClient dockerClient;

  private final TaskManager taskManager;

  public FlowerDockerService(DockerClient dockerClient, @Autowired TaskManager taskManager) {
    this.dockerClient = dockerClient;
    this.taskManager = taskManager;
  }

  public void copyCompressedDataToContainer(String containerName, String destDir, TarFile tar) {
    try {
      createDirectory(containerName, destDir);
      streamTarToContainer(containerName, destDir, tar);
    } catch (NotFoundException e) {
      throw new ContainerNotFoundException(containerName, e);
    } catch (DockerException | IOException e) {
      throw new DataPushFailedException(containerName, e);
    }
  }

  private void pollCompletion(ExecCreateCmdResponse exec) throws InterruptedException {
    dockerClient
        .execStartCmd(exec.getId())
        .exec(new ResultCallback.Adapter<Frame>())
        .awaitCompletion();
  }

  public void createDirectory(String dockerContainerName, String dir) throws IOException {
    ExecCreateCmdResponse exec =
        dockerClient
            .execCreateCmd(dockerContainerName)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .withCmd("mkdir", "-p", dir)
            .exec();

    try {
      pollCompletion(exec);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating directory " + dir, e);
    }
    Long exitCode = dockerClient.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
    if (exitCode == null || exitCode != 0) {
      throw new IOException("Failed to create directory " + dir + " (exit code " + exitCode + ")");
    }
  }

  private void streamTarToContainer(String dockerContainerName, String destDir, TarFile tar)
      throws IOException {
    PipedInputStream tarStream = tar.getTarStream();
    FutureTask<Void> writer = taskManager.create(tar::write);
    taskManager.start(writer);
    try (tarStream) {
      dockerClient
          .copyArchiveToContainerCmd(dockerContainerName)
          .withTarInputStream(tarStream)
          .withRemotePath(destDir)
          .exec();
    }
    taskManager.await(writer);
  }
}
