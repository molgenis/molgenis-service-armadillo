package org.molgenis.connection.flower.helpers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskManagerTest {

  private TaskManager taskManager;

  @BeforeEach
  void setUp() {
    taskManager = new TaskManager();
  }

  @AfterEach
  void clearInterruptFlag() {
    // make sure a failing interrupt test does not leak the flag into other tests
    Thread.interrupted();
  }

  @Test
  void createReturnsNotYetStartedTask() {
    @SuppressWarnings("unchecked")
    Callable<Void> callable = mock(Callable.class);

    FutureTask<Void> task = taskManager.create(callable);

    assertNotNull(task);
    assertFalse(task.isDone());
    verifyNoInteractions(callable);
  }

  @Test
  void createReturnsNewTaskOnEachCall() {
    Callable<Void> callable = () -> null;

    FutureTask<Void> first = taskManager.create(callable);
    FutureTask<Void> second = taskManager.create(callable);

    assertTrue(first != second);
  }

  @Test
  void startRunsTask() throws Exception {
    CountDownLatch ran = new CountDownLatch(1);
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              ran.countDown();
              return null;
            });

    taskManager.start(task);

    assertTrue(ran.await(5, TimeUnit.SECONDS));
    assertNull(task.get(5, TimeUnit.SECONDS));
    assertTrue(task.isDone());
  }

  @Test
  void startRunsTaskOnVirtualThread() throws Exception {
    AtomicReference<Thread> runner = new AtomicReference<>();
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              runner.set(Thread.currentThread());
              return null;
            });

    taskManager.start(task);
    task.get(5, TimeUnit.SECONDS);

    assertNotNull(runner.get());
    assertTrue(runner.get().isVirtual());
    assertNotSame(runner.get(), Thread.currentThread());
  }

  @Test
  void startDoesNotBlockCaller() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    AtomicBoolean finished = new AtomicBoolean(false);
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              release.await();
              finished.set(true);
              return null;
            });

    taskManager.start(task);

    // start() returned while task is still blocked
    assertFalse(task.isDone());
    assertFalse(finished.get());

    release.countDown();
    task.get(5, TimeUnit.SECONDS);
    assertTrue(finished.get());
  }

  @Test
  void awaitReturnsWhenTaskCompletes() throws Exception {
    AtomicBoolean ran = new AtomicBoolean(false);
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              ran.set(true);
              return null;
            });
    taskManager.start(task);

    taskManager.await(task);

    assertTrue(ran.get());
    assertTrue(task.isDone());
  }

  @Test
  void awaitReturnsImmediatelyForAlreadyCompletedTask() throws Exception {
    FutureTask<Void> task = taskManager.create(() -> null);
    task.run();

    taskManager.await(task);

    assertTrue(task.isDone());
  }

  @Test
  void awaitWrapsExecutionExceptionInIOException() {
    IllegalStateException cause = new IllegalStateException("boom");
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              throw cause;
            });
    taskManager.start(task);

    IOException exception = assertThrows(IOException.class, () -> taskManager.await(task));

    assertEquals("Failed to write tar stream", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void awaitPreservesCheckedExceptionAsCause() {
    IOException cause = new IOException("disk full");
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              throw cause;
            });
    task.run();

    IOException exception = assertThrows(IOException.class, () -> taskManager.await(task));

    assertEquals("Failed to write tar stream", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void awaitWrapsInterruptedExceptionAndRestoresInterruptFlag() {
    // never started, so get() blocks until interrupted
    FutureTask<Void> task = taskManager.create(() -> null);
    Thread.currentThread().interrupt();

    IOException exception = assertThrows(IOException.class, () -> taskManager.await(task));

    assertEquals("Interrupted while writing tar stream", exception.getMessage());
    assertInstanceOf(InterruptedException.class, exception.getCause());
    assertTrue(Thread.currentThread().isInterrupted());
  }

  @Test
  void awaitRestoresInterruptFlagWhenInterruptedWhileWaiting() throws Exception {
    CountDownLatch release = new CountDownLatch(1);
    FutureTask<Void> task =
        taskManager.create(
            () -> {
              release.await();
              return null;
            });
    taskManager.start(task);

    AtomicReference<IOException> thrown = new AtomicReference<>();
    AtomicBoolean interruptFlagAfter = new AtomicBoolean(false);
    Thread waiter =
        new Thread(
            () -> {
              try {
                taskManager.await(task);
              } catch (IOException e) {
                thrown.set(e);
                interruptFlagAfter.set(Thread.currentThread().isInterrupted());
              }
            });
    waiter.start();
    // give the waiter time to block in task.get()
    Thread.sleep(100);
    waiter.interrupt();
    waiter.join(5000);

    try {
      assertNotNull(thrown.get());
      assertEquals("Interrupted while writing tar stream", thrown.get().getMessage());
      assertTrue(interruptFlagAfter.get());
    } finally {
      release.countDown();
    }
  }
}
