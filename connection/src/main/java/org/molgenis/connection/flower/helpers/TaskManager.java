package org.molgenis.connection.flower.helpers;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import org.springframework.stereotype.Component;

@Component
public class TaskManager {
  /** Helper class for FLowerContainerConnection */
  public FutureTask<Void> create(Callable<Void> task) {
    return new FutureTask<>(task);
  }

  public void await(FutureTask<Void> task) throws IOException {
    try {
      task.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while writing tar stream", e);
    } catch (ExecutionException e) {
      throw new IOException("Failed to write tar stream", e.getCause());
    }
  }

  public void start(FutureTask<Void> task) {
    Thread.ofVirtual().start(task);
  }
}
