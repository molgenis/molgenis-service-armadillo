package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultRebootScriptRunnerTest {

  @TempDir Path tempDir;

  DefaultRebootScriptRunner scriptRunner;
  private File logFile;
  private File errorLogFile;

  @BeforeEach
  void setUp() {
    logFile = tempDir.resolve("update.log").toFile();
    errorLogFile = tempDir.resolve("update-error.log").toFile();
    scriptRunner = new DefaultRebootScriptRunner(logFile.getAbsolutePath());
  }

  @AfterEach
  void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  void getProcessBuilder_setsCommandAndRedirectsToLogFiles() {
    ProcessBuilder pb =
        scriptRunner.getProcessBuilder(
            logFile.getAbsolutePath(), "armadillo-reboot.sh", "-v", "5.14.0");

    assertThat(pb.command()).containsExactly("armadillo-reboot.sh", "-v", "5.14.0");
    assertThat(pb.redirectOutput().file()).isEqualTo(logFile);
    assertThat(pb.redirectError().file()).isEqualTo(errorLogFile);
  }

  @Test
  void getUpdateLogFile_createsFileIfMissing() throws IOException {
    assertFalse(logFile.exists());
    scriptRunner.runRebootScript("true");
    assertTrue(logFile.exists());
  }

  @Test
  void getUpdateLogFile_throwsException() {
    // create a regular file, then treat it as if it were a parent directory
    File blockingFile = tempDir.resolve("not-a-directory").toFile();
    assertDoesNotThrow(() -> assertTrue(blockingFile.createNewFile()));

    String badLogPath = blockingFile.toPath().resolve("update.log").toString();
    scriptRunner = new DefaultRebootScriptRunner(badLogPath);

    assertThrows(IOException.class, () -> scriptRunner.runRebootScript("true"));
  }

  @Test
  void runRebootScript_startsNonDaemonThreadWithExpectedName() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    runner.setProcessBuilderOverride(new ProcessBuilder("true"));

    runner.runRebootScript("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread, "expected background thread to have been started");
    assertFalse(updateThread.isDaemon());

    updateThread.join(2000);
  }

  @Test
  void runRebootScript_completesSuccessfullyWithoutUncaughtException() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    runner.setProcessBuilderOverride(new ProcessBuilder("true"));

    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.set(e));

    runner.runRebootScript("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread);
    updateThread.join(2000);

    assertNull(uncaught.get());
  }

  @Test
  void runRebootScript_wrapsIOExceptionFromProcessStart() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    // a command that cannot be started -> ProcessBuilder.start() throws IOException
    runner.setProcessBuilderOverride(new ProcessBuilder("definitely-not-a-real-binary-xyz"));

    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.set(e));

    runner.runRebootScript("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread);
    updateThread.join(2000);

    assertThat(uncaught.get()).isInstanceOf(RuntimeException.class);
    assertThat(uncaught.get().getCause()).isInstanceOf(IOException.class);
  }

  private static Thread findThreadByName(String name) {
    ThreadGroup root = Thread.currentThread().getThreadGroup();
    while (root.getParent() != null) {
      root = root.getParent();
    }
    Thread[] threads = new Thread[root.activeCount() * 2 + 16];
    int count = root.enumerate(threads, true);
    for (int i = 0; i < count; i++) {
      if (name.equals(threads[i].getName())) {
        return threads[i];
      }
    }
    return null;
  }

  /** Lets tests swap in a controllable ProcessBuilder instead of the real one built from args. */
  private static class ProcessBuilderOverridingRunner extends DefaultRebootScriptRunner {
    private ProcessBuilder processBuilderOverride;

    ProcessBuilderOverridingRunner(String configuredLogPath) {
      super(configuredLogPath);
    }

    void setProcessBuilderOverride(ProcessBuilder pb) {
      this.processBuilderOverride = pb;
    }

    @Override
    ProcessBuilder getProcessBuilder(String logFilePath, String... args) {
      return processBuilderOverride != null
          ? processBuilderOverride
          : super.getProcessBuilder(logFilePath, args);
    }
  }
}
