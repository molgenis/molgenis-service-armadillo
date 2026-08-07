package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.armadillo.exceptions.RebootScriptRunFailedException;

class PythonRebootScriptRunnerTest {

  @TempDir Path tempDir;

  RecordingPythonRebootScriptRunner scriptRunner;
  private File logFile;

  @BeforeEach
  void setUp() {
    logFile = tempDir.resolve("update.log").toFile();
    scriptRunner = new RecordingPythonRebootScriptRunner(logFile.getAbsolutePath());
  }

  @AfterEach
  void tearDown() {
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  void runScriptInDifferentThread_startsNonDaemonThreadWithExpectedName() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    runner.setProcessBuilderOverride(new ProcessBuilder("true"));

    runner.runScriptInDifferentThread("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread, "expected background thread to have been started");
    assertFalse(updateThread.isDaemon());

    updateThread.join(2000);
  }

  @Test
  void runScriptInDifferentThread_completesSuccessfullyWithoutUncaughtException() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    runner.setProcessBuilderOverride(new ProcessBuilder("true"));

    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.set(e));

    runner.runScriptInDifferentThread("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread);
    updateThread.join(2000);

    assertNull(uncaught.get());
  }

  @Test
  void runScriptInDifferentThread_wrapsIOExceptionFromProcessStart() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    // a command that cannot be started -> ProcessBuilder.start() throws IOException
    runner.setProcessBuilderOverride(new ProcessBuilder("definitely-not-a-real-binary-xyz"));

    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.set(e));

    runner.runScriptInDifferentThread("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread);
    updateThread.join(2000);

    assertThat(uncaught.get()).isInstanceOf(RebootScriptRunFailedException.class);
    assertThat(uncaught.get().getMessage()).startsWith("Reboot script failed:");
  }

  @Test
  void runScriptInDifferentThread_handlesInterruptionDuringSleep() throws Exception {
    ProcessBuilderOverridingRunner runner =
        new ProcessBuilderOverridingRunner(logFile.getAbsolutePath());
    runner.setProcessBuilderOverride(new ProcessBuilder("true"));

    AtomicReference<Throwable> uncaught = new AtomicReference<>();
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> uncaught.set(e));

    runner.runScriptInDifferentThread("ignored");

    Thread updateThread = findThreadByName("update-armadillo");
    assertNotNull(updateThread);
    updateThread.interrupt(); // lands inside Thread.sleep(200)

    updateThread.join(2000);

    assertThat(uncaught.get()).isInstanceOf(RebootScriptRunFailedException.class);
    assertThat(uncaught.get().getMessage()).contains("Script run interrupted");
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

  private static class RecordingPythonRebootScriptRunner extends PythonRebootScriptRunner {
    private String lastScriptExecution;

    public RecordingPythonRebootScriptRunner(String configuredLogPath) {
      super(configuredLogPath);
    }

    @Override
    void runScriptInDifferentThread(String pythonScript) {
      lastScriptExecution = pythonScript;
    }

    public String getLastScriptExecution() {
      return lastScriptExecution;
    }
  }

  /** Lets tests swap in a controllable ProcessBuilder instead of the real "python3 -c ..." one. */
  private static class ProcessBuilderOverridingRunner extends PythonRebootScriptRunner {
    private ProcessBuilder processBuilderOverride;

    ProcessBuilderOverridingRunner(String configuredLogPath) {
      super(configuredLogPath);
    }

    void setProcessBuilderOverride(ProcessBuilder pb) {
      this.processBuilderOverride = pb;
    }

    @Override
    ProcessBuilder getProcessBuilderForPythonScript(String pythonScript) {
      return processBuilderOverride != null
          ? processBuilderOverride
          : super.getProcessBuilderForPythonScript(pythonScript);
    }
  }
}
