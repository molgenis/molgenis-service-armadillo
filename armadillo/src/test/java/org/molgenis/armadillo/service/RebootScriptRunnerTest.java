package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.armadillo.TestHelpers.setField;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RebootScriptRunnerTest {
  @TempDir Path tempDir;

  RebootScriptRunner scriptRunner;

  @BeforeEach
  void setUp() throws Exception {
    File logFile = tempDir.resolve("test.log").toFile();
    String jarHome = tempDir.resolve("armadillo.jar").toString();
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
  void getUpdateLogFile_createsFileIfMissing() throws Exception {
    Path logPath = tempDir.resolve("logs/update.log");
    setField(scriptRunner, "logPath", logPath.toString());

    Method m = RebootScriptRunner.class.getDeclaredMethod("getUpdateLogFile");
    m.setAccessible(true);
    File scriptLogFile = (File) m.invoke(scriptRunner);

    assertTrue(scriptLogFile.exists());
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
                "['/usr/share/armadillo/armadillo-reboot.sh', '-v', '5.14.0']",
                "./my-log.txt");

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
        (String)
            m.invoke(
                scriptRunner,
                "['/usr/share/armadillo/armadillo-reboot.sh', '-v', '5.14.0', '-u']",
                "./my-log.txt");
    assertTrue(script.contains("'-u'"));
  }

  @Test
  void createPythonScript_containsMode() throws Exception {
    Method m =
        RebootScriptRunner.class.getDeclaredMethod(
            "createPythonScriptForThread", String.class, String.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(
                scriptRunner,
                "['/usr/share/armadillo/armadillo-reboot.sh', '-m', 'PROD']",
                "./my-log.txt");
    assertTrue(script.contains("PROD")); // mode injected via -m
  }
}
