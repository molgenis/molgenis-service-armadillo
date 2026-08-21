package org.molgenis.armadillo.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.molgenis.armadillo.exceptions.RebootScriptRunFailedException;

public class PythonRebootScriptRunner implements RebootScriptRunner {

  private final String logPath;

  public PythonRebootScriptRunner(String configuredLogPath) {
    var splitLogFilepath = configuredLogPath.split(Pattern.quote(File.separator));
    this.logPath =
        String.join(File.separator, Arrays.copyOf(splitLogFilepath, splitLogFilepath.length - 1))
            + File.separator
            + "update.log";
  }

  private File getUpdateLogFile() throws IOException {
    File logFile = new File(logPath);
    logFile.getParentFile().mkdirs();

    if (!logFile.exists()) {
      boolean fileCreated = logFile.createNewFile();
      if (!fileCreated) {
        throw new IOException("File cannot be created");
      }
    }
    return logFile;
  }

  private String createPythonScriptForThread(String pythonList, String absoluteLogFilePath) {
    String scriptTemplate =
        """
        import os, sys, subprocess
        if os.fork() > 0:
            sys.exit(0)
        os.setsid()
        if os.fork() > 0:
            sys.exit(0)
        with open('%s', 'a') as log:
            subprocess.run(%s, stdout=log, stderr=log, stdin=subprocess.DEVNULL)
        """;
    return String.format(scriptTemplate, absoluteLogFilePath, pythonList);
  }

  // The only arguments that get injected are injected via application.yml from variables that
  // cannot otherwise be changed.
  // /dev/null is the actual path that the input will need to be redirected to
  @java.lang.SuppressWarnings({"java:S4036", "java:S1075"})
  ProcessBuilder getProcessBuilderForPythonScript(String pythonScript) {
    ProcessBuilder processBuilder = new ProcessBuilder("python3", "-c", pythonScript);
    processBuilder.redirectInput(new File("/dev/null"));
    processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    return processBuilder;
  }

  @Override
  public void runRebootScript(String... args) throws IOException {
    String pythonCommand = buildPythonCommand(args);
    String pythonScript =
        createPythonScriptForThread(pythonCommand, getUpdateLogFile().getAbsolutePath());
    runScriptInDifferentThread(pythonScript);
  }

  // Builds a Python list literal e.g. ['/path/script', '-p', '/home']
  String buildPythonCommand(String... args) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      sb.append("'").append(args[i].replace("'", "\\'")).append("'");
      if (i < args.length - 1) sb.append(", ");
    }
    sb.append("]");
    return sb.toString();
  }

  // the thread cant be child process of application thread, so virtual threads impossible
  @java.lang.SuppressWarnings({"java:S6881"})
  void runScriptInDifferentThread(String pythonScript) {
    Thread updateThread =
        new Thread(
            () -> {
              try {
                Thread.sleep(200);
                ProcessBuilder processBuilder = getProcessBuilderForPythonScript(pythonScript);
                Process python = processBuilder.start();
                python.waitFor();
              } catch (IOException e) {
                throw new RebootScriptRunFailedException("Script run failed:" + e.getMessage(), e);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RebootScriptRunFailedException(
                    "Script run interrupted:" + e.getMessage(), e);
              }
            });
    updateThread.setDaemon(false);
    updateThread.setName("update-armadillo");
    updateThread.start();
  }
}
