package org.molgenis.armadillo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.armadillo.TestHelpers.setField;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.info.BuildProperties;

public class RebootScriptRunnerTest {
  BuildProperties buildProperties;

  @TempDir Path tempDir;

  private File logFile;
  private String jarHome;
  RebootScriptRunner scriptRunner;

  @BeforeEach
  void setUp() throws Exception {
    logFile = tempDir.resolve("test.log").toFile();
    logFile.createNewFile();
    jarHome = tempDir.resolve("armadillo.jar").toString();
    scriptRunner = new RebootScriptRunner(logFile.getAbsolutePath(), jarHome);
  }

  @Test
  void getProcessBuilderForRebootScript() {
    String pythonScript = "print('hello world')";
    ProcessBuilder pb = scriptRunner.getProcessBuilderForPythonScript(pythonScript);
    assertThat(pb.command().get(2)).isEqualTo(pythonScript);
    assertThat(pb.redirectInput().file()).isEqualTo(new File("/dev/null"));
  }

  @Test
  void thread_should_have_correct_name_and_be_daemon() throws Exception {
    Thread tailer = scriptRunner.startLogTailer(logFile, line -> {});

    assertThat(tailer.getName()).isEqualTo("update-log-tailer");
    assertThat(tailer.isDaemon()).isTrue();
    assertThat(tailer.isAlive()).isTrue();

    tailer.interrupt();
  }

  @Test
  void should_pick_up_lines_written_after_start() throws Exception {
    List<String> captured = new CopyOnWriteArrayList<>();

    Thread tailer = scriptRunner.startLogTailer(logFile, captured::add);

    // Write lines AFTER the tailer has started (it skips existing content)
    await().atMost(500, TimeUnit.MILLISECONDS).until(() -> tailer.isAlive());

    try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
      writer.println("line one");
      writer.println("line two");
      writer.flush();
    }

    await().atMost(2, TimeUnit.SECONDS).until(() -> captured.size() >= 2);

    assertThat(captured).containsExactly("line one", "line two");

    tailer.interrupt();
  }

  @Test
  void should_skip_content_already_in_file_at_start() throws Exception {
    // Write content BEFORE starting the tailer
    Files.writeString(logFile.toPath(), "pre-existing line\n");

    List<String> captured = new CopyOnWriteArrayList<>();
    Thread tailer = scriptRunner.startLogTailer(logFile, captured::add);

    // Give it time to potentially (wrongly) pick up the old line
    Thread.sleep(300);

    assertThat(captured).isEmpty();

    tailer.interrupt();
  }

  @Test
  void should_stop_cleanly_on_interrupt() throws Exception {
    Thread tailer = scriptRunner.startLogTailer(logFile, line -> {});

    tailer.interrupt();

    await().atMost(1, TimeUnit.SECONDS).until(() -> !tailer.isAlive());

    assertThat(tailer.isAlive()).isFalse();
  }

  @Test
  void should_log_error_on_missing_file() throws Exception {
    File missing = tempDir.resolve("nonexistent.log").toFile();

    Thread tailer = scriptRunner.startLogTailer(missing, line -> {});

    tailer.join(5000); // wait up to 5s for the thread to die naturally

    assertThat(tailer.isAlive()).isFalse();
  }

  @Test
  void getUpdateLogFile_createsFileIfMissing() throws Exception {
    Path logPath = tempDir.resolve("logs/update.log");
    setField(scriptRunner, "logPath", logPath.toString());

    Method m = RebootScriptRunner.class.getDeclaredMethod("getUpdateLogFile");
    m.setAccessible(true);
    File logFile = (File) m.invoke(scriptRunner);

    assertTrue(logFile.exists());
  }

  @Test
  void buildPythonCommand_formatsCorrectly() throws Exception {
    Method m = RebootScriptRunner.class.getDeclaredMethod("buildPythonCommand", String[].class);
    m.setAccessible(true);

    String result =
        (String) m.invoke(scriptRunner, (Object) new String[] {"/path/script", "-p", "/home"});
    assertEquals("['/path/script', '-p', '/home']", result);
  }

  @Test
  void buildPythonCommand_escapesSingleQuotes() throws Exception {
    Method m = RebootScriptRunner.class.getDeclaredMethod("buildPythonCommand", String[].class);
    m.setAccessible(true);

    String result = (String) m.invoke(scriptRunner, (Object) new String[] {"it's"});
    assertEquals("['it\\'s']", result);
  }

  @Test
  void createPythonScript_nonUpdateBranch_doesNotContainUpdateFlag() throws Exception {
    Method m =
        RebootScriptRunner.class.getDeclaredMethod(
            "createPythonScriptForThread", String.class, String.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(
                scriptRunner,
                // this is not how you call this method exactly, but invoke cannot handle more
                // arguments
                // than the method takes (method takes String...)
                "/usr/share/armadillo/armadillo-reboot.sh",
                "-v 5.14.0");

    assertTrue(script.contains("import os, sys, subprocess"));
    assertTrue(script.contains("5.14.0"));
    // The '-u' update flag must NOT be present when isUpdate=false
    assertFalse(script.contains("'-u'"));
  }

  @Test
  void createPythonScript_updateBranch_containsUpdateFlag() throws Exception {
    Method m =
        RebootScriptRunner.class.getDeclaredMethod(
            "createPythonScriptForThread", String.class, String.class);
    m.setAccessible(true);
    String script =
        (String) m.invoke(scriptRunner, "/usr/share/armadillo/armadillo-reboot.sh", "-v 5.14.0 -u");
    assertTrue(script.contains("'-u'"));
  }

  @Test
  void createPythonScript_containsArmadilloHomeVersionAndMode() throws Exception {
    Method m =
        RebootScriptRunner.class.getDeclaredMethod(
            "createPythonScriptForThread", String.class, String.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(scriptRunner, "/usr/share/armadillo/armadillo-reboot.sh", "-m PROD -v 5.14.0");
    assertTrue(script.contains("5.14.0")); // version injected via -v
    assertTrue(script.contains("PROD")); // mode injected via -m
  }
}
