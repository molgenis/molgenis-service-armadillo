package org.molgenis.armadillo.controller;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;
import org.apache.logging.log4j.LoggingException;

public class RebootScriptRunner {
  String logPath;
  String jarHome;

  public RebootScriptRunner(String updateLogPath, String jarHome) {
    this.logPath = updateLogPath;
    this.jarHome = jarHome;
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

  String createPythonScriptForThread(String pythonList, String absoluteLogFilePath) {
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

  // the only arguments that get injected are injected via application.yml from variables that
  // cannot otherwise be changed,
  // /dev/null is the actual path that the input will need to be redirected to
  @java.lang.SuppressWarnings({"squid:S4036", "squid:S1075"})
  ProcessBuilder getProcessBuilderForPythonScript(String pythonScript) {
    ProcessBuilder processBuilder = new ProcessBuilder("python3", "-c", pythonScript);
    processBuilder.redirectInput(new File("/dev/null"));
    processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
    return processBuilder;
  }

  public void runRebootScript(String... args) throws IOException {
    String pythonCommand = buildPythonCommand(args);
    String pythonScript =
        createPythonScriptForThread(pythonCommand, getUpdateLogFile().getAbsolutePath());
    runScriptInDifferentThread(pythonScript);
  }

  Thread startLogTailer(File logFile, Consumer<String> lineHandler) {
    Thread tailer =
        new Thread(
            () -> {
              try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                reader.skip(logFile.length());
                while (!Thread.currentThread().isInterrupted()) {
                  String line = reader.readLine();
                  if (line == null) {
                    Thread.sleep(100);
                  } else {
                    lineHandler.accept(line); // <-- add this
                  }
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } catch (IOException e) {
                throw new LoggingException(
                    format("[UPDATE SCRIPT]: Log tailer error: %s\"", e.getMessage()));
              }
            });
    tailer.setDaemon(true);
    tailer.setName("update-log-tailer");
    tailer.start();
    return tailer;
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

  void runScriptInDifferentThread(String pythonScript) {
    Thread updateThread =
        new Thread(
            () -> {
              try {
                File logFile = getUpdateLogFile();
                Thread logTailer = startLogTailer(logFile, line -> {});
                Thread.sleep(200);
                ProcessBuilder processBuilder = getProcessBuilderForPythonScript(pythonScript);
                Process python = processBuilder.start();
                python.waitFor();
                logTailer.join(5000);
              } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Script run failed:", e);
              }
            });
    updateThread.setDaemon(false);
    updateThread.setName("update-armadillo");
    updateThread.start();
  }
}
