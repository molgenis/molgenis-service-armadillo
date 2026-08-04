package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PythonRebootScriptRunnerTest {
  @TempDir Path tempDir;

  RecordingPythonRebootScriptRunner scriptRunner;
  private File logFile;

  @BeforeEach
  void setUp() {
    logFile = tempDir.resolve("update.log").toFile();
    String jarHome = tempDir.resolve("armadillo.jar").toString();
    scriptRunner = new RecordingPythonRebootScriptRunner(logFile.getAbsolutePath(), jarHome);
  }

  @Test
  void getProcessBuilderForRebootScript() {
    String pythonScript = "print('hello world')";
    ProcessBuilder pb = scriptRunner.getProcessBuilderForPythonScript(pythonScript);
    assertThat(pb.command().get(2)).isEqualTo(pythonScript);
    assertThat(pb.redirectInput().file()).isEqualTo(new File("/dev/null"));
  }

  @Test
  void getUpdateLogFile_createsFileIfMissing() throws IOException {
    assertFalse(logFile.exists());
    scriptRunner.runRebootScript();
    assertTrue(logFile.exists());
  }

  @Test
  void createPythonScript_nonUpdateBranch_doesNotContainUpdateFlag() throws IOException {
    scriptRunner.runRebootScript("armadillo-reboot.sh", "-v", "5.14.0");
    String script = scriptRunner.getLastScriptExecution();

    assertTrue(script.contains("import os, sys, subprocess"));
    assertTrue(script.contains("with open('" + logFile.getAbsolutePath() + "', 'a') as log:"));
    assertTrue(
        script.contains(
            "subprocess.run(['armadillo-reboot.sh', '-v', '5.14.0'], stdout=log, stderr=log, stdin=subprocess.DEVNULL"));
  }

  private static class RecordingPythonRebootScriptRunner extends PythonRebootScriptRunner {

    private String lastScriptExecution;

    public RecordingPythonRebootScriptRunner(String configuredLogPath, String jarHome) {
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
}
