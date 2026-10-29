package org.molgenis.connection.flower.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectExecCmd;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.connection.exceptions.ContainerNotFoundException;
import org.molgenis.connection.exceptions.DataPushFailedException;
import org.molgenis.connection.flower.helpers.TarFile;
import org.molgenis.connection.flower.helpers.TaskManager;

@ExtendWith(MockitoExtension.class)
class FlowerDockerServiceTest {

  private static final String CONTAINER = "flower-container";
  private static final String DEST_DIR = "/data/incoming";
  private static final String EXEC_ID = "exec-123";

  @Mock private DockerClient dockerClient;
  @Mock private TaskManager taskManager;

  @Mock(answer = Answers.RETURNS_SELF)
  private ExecCreateCmd execCreateCmd;

  @Mock private ExecCreateCmdResponse execCreateResponse;
  @Mock private ExecStartCmd execStartCmd;
  @Mock private ResultCallback.Adapter<Frame> callback;
  @Mock private InspectExecCmd inspectExecCmd;
  @Mock private InspectExecResponse inspectExecResponse;

  @Mock(answer = Answers.RETURNS_SELF)
  private CopyArchiveToContainerCmd copyCmd;

  @Mock private TarFile tar;
  @Mock private PipedInputStream tarStream;
  @Mock private FutureTask<Void> writer;

  private FlowerDockerService service;

  @BeforeEach
  void setUp() {
    service = new FlowerDockerService(dockerClient, taskManager);
  }

  @AfterEach
  void clearInterruptFlag() {
    // don't leak the interrupt flag from the interrupt test into other tests
    Thread.interrupted();
  }

  /** Stubs the whole mkdir exec chain so it completes with the given exit code. */
  private void stubMkdirExec(Long exitCode) {
    stubExecCreate();
    stubExecStart();
    stubInspect(exitCode);
  }

  private void stubExecCreate() {
    when(dockerClient.execCreateCmd(CONTAINER)).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenReturn(execCreateResponse);
    when(execCreateResponse.getId()).thenReturn(EXEC_ID);
  }

  private void stubExecStart() {
    when(dockerClient.execStartCmd(EXEC_ID)).thenReturn(execStartCmd);
    // exec(T) is generic, so use doReturn to sidestep the type inference
    doReturn(callback).when(execStartCmd).exec(any());
  }

  private void stubInspect(Long exitCode) {
    when(dockerClient.inspectExecCmd(EXEC_ID)).thenReturn(inspectExecCmd);
    when(inspectExecCmd.exec()).thenReturn(inspectExecResponse);
    when(inspectExecResponse.getExitCodeLong()).thenReturn(exitCode);
  }

  private void stubTarStreaming() {
    when(tar.getTarStream()).thenReturn(tarStream);
    when(taskManager.create(any())).thenReturn(writer);
    when(dockerClient.copyArchiveToContainerCmd(CONTAINER)).thenReturn(copyCmd);
  }

  @Test
  void createDirectoryRunsMkdirInContainer() throws Exception {
    stubMkdirExec(0L);

    service.createDirectory(CONTAINER, DEST_DIR);

    verify(dockerClient).execCreateCmd(CONTAINER);
    verify(execCreateCmd).withAttachStdout(true);
    verify(execCreateCmd).withAttachStderr(true);
    verify(execCreateCmd).withCmd("mkdir", "-p", DEST_DIR);
  }

  @Test
  void createDirectoryStartsExecAndWaitsForCompletion() throws Exception {
    stubMkdirExec(0L);

    service.createDirectory(CONTAINER, DEST_DIR);

    verify(dockerClient).execStartCmd(EXEC_ID);
    verify(callback).awaitCompletion();
  }

  @Test
  void createDirectoryChecksExitCodeAfterCompletion() throws Exception {
    stubMkdirExec(0L);

    service.createDirectory(CONTAINER, DEST_DIR);

    InOrder order = inOrder(callback, dockerClient);
    order.verify(callback).awaitCompletion();
    order.verify(dockerClient).inspectExecCmd(EXEC_ID);
  }

  @Test
  void createDirectoryDoesNotThrowOnExitCodeZero() throws Exception {
    stubMkdirExec(0L);

    assertDoesNotThrow(() -> service.createDirectory(CONTAINER, DEST_DIR));
  }

  @Test
  void createDirectoryThrowsOnNonZeroExitCode() throws Exception {
    stubMkdirExec(1L);

    IOException e =
        assertThrows(IOException.class, () -> service.createDirectory(CONTAINER, DEST_DIR));

    assertEquals("Failed to create directory " + DEST_DIR + " (exit code 1)", e.getMessage());
  }

  @Test
  void createDirectoryThrowsOnNullExitCode() throws Exception {
    stubMkdirExec(null);

    IOException e =
        assertThrows(IOException.class, () -> service.createDirectory(CONTAINER, DEST_DIR));

    assertEquals("Failed to create directory " + DEST_DIR + " (exit code null)", e.getMessage());
  }

  @Test
  void createDirectoryWrapsInterruptAndRestoresInterruptFlag() throws Exception {
    stubExecCreate();
    stubExecStart();
    InterruptedException interrupted = new InterruptedException("stop");
    when(callback.awaitCompletion()).thenThrow(interrupted);

    IOException e =
        assertThrows(IOException.class, () -> service.createDirectory(CONTAINER, DEST_DIR));

    assertEquals("Interrupted while creating directory " + DEST_DIR, e.getMessage());
    assertSame(interrupted, e.getCause());
    assertTrue(Thread.currentThread().isInterrupted());
    // exit code must not be inspected when interrupted
    verify(dockerClient, never()).inspectExecCmd(any());
  }

  @Test
  void copyCreatesDirectoryThenStreamsTar() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();

    service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar);

    InOrder order = inOrder(execCreateCmd, copyCmd);
    order.verify(execCreateCmd).withCmd("mkdir", "-p", DEST_DIR);
    order.verify(copyCmd).exec();
  }

  @Test
  void copyPassesStreamAndRemotePathToDocker() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();

    service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar);

    verify(dockerClient).copyArchiveToContainerCmd(CONTAINER);
    verify(copyCmd).withTarInputStream(tarStream);
    verify(copyCmd).withRemotePath(DEST_DIR);
    verify(copyCmd).exec();
  }

  @Test
  void copyStartsWriterBeforeDockerReadsAndAwaitsAfter() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();

    service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar);

    InOrder order = inOrder(taskManager, copyCmd);
    order.verify(taskManager).start(writer);
    order.verify(copyCmd).exec();
    order.verify(taskManager).await(writer);
  }

  @Test
  void copyWriterTaskInvokesTarWrite() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();

    service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Callable<Void>> captor = ArgumentCaptor.forClass(Callable.class);
    verify(taskManager).create(captor.capture());
    verify(tar, never()).write();

    captor.getValue().call();

    verify(tar).write();
  }

  @Test
  void copyClosesTarStream() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();

    service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar);

    verify(tarStream).close();
  }

  @Test
  void copyClosesTarStreamEvenWhenDockerCopyFails() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();
    doThrow(new DockerException("copy failed", 500)).when(copyCmd).exec();

    assertThrows(
        DataPushFailedException.class,
        () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    verify(tarStream).close();
    verify(taskManager, never()).await(any());
  }

  @Test
  void copyThrowsContainerNotFoundWhenExecCreateReportsNotFound() {
    NotFoundException notFound = new NotFoundException("no such container");
    when(dockerClient.execCreateCmd(CONTAINER)).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenThrow(notFound);

    ContainerNotFoundException e =
        assertThrows(
            ContainerNotFoundException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertSame(notFound, e.getCause());
    verifyNoInteractions(taskManager);
  }

  @Test
  void copyThrowsContainerNotFoundWhenCopyReportsNotFound() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();
    NotFoundException notFound = new NotFoundException("no such container");
    doThrow(notFound).when(copyCmd).exec();

    ContainerNotFoundException e =
        assertThrows(
            ContainerNotFoundException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertSame(notFound, e.getCause());
  }

  @Test
  void copyThrowsDataPushFailedOnGenericDockerException() {
    DockerException docker = new DockerException("daemon error", 500);
    when(dockerClient.execCreateCmd(CONTAINER)).thenReturn(execCreateCmd);
    when(execCreateCmd.exec()).thenThrow(docker);

    DataPushFailedException e =
        assertThrows(
            DataPushFailedException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertSame(docker, e.getCause());
  }

  @Test
  void copyThrowsDataPushFailedWhenMkdirFails() throws Exception {
    stubMkdirExec(2L);

    DataPushFailedException e =
        assertThrows(
            DataPushFailedException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertInstanceOf(IOException.class, e.getCause());
    // tar must not be streamed when the directory could not be created
    verifyNoInteractions(taskManager);
    verify(dockerClient, never()).copyArchiveToContainerCmd(any());
  }

  @Test
  void copyThrowsDataPushFailedWhenWriterFails() throws Exception {
    stubMkdirExec(0L);
    stubTarStreaming();
    IOException writerFailure = new IOException("Failed to write tar stream");
    doThrow(writerFailure).when(taskManager).await(writer);

    DataPushFailedException e =
        assertThrows(
            DataPushFailedException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertSame(writerFailure, e.getCause());
  }

  @Test
  void copyThrowsDataPushFailedWhenInterruptedWhileCreatingDirectory() throws Exception {
    stubExecCreate();
    stubExecStart();
    when(callback.awaitCompletion()).thenThrow(new InterruptedException());

    DataPushFailedException e =
        assertThrows(
            DataPushFailedException.class,
            () -> service.copyCompressedDataToContainer(CONTAINER, DEST_DIR, tar));

    assertInstanceOf(IOException.class, e.getCause());
    assertInstanceOf(InterruptedException.class, e.getCause().getCause());
    assertTrue(Thread.currentThread().isInterrupted());
  }
}
