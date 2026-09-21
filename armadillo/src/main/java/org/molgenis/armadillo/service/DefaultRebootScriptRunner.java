package org.molgenis.armadillo.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class DefaultRebootScriptRunner implements RebootScriptRunner {

  private final String logPath;

  public DefaultRebootScriptRunner(String configuredLogPath) {
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

  // The only arguments that get injected are injected via application.yml from variables that
  // cannot otherwise be changed.
  // /dev/null is the actual path that the input will need to be redirected to
  @java.lang.SuppressWarnings({"java:S4036", "java:S1075"})
  ProcessBuilder getProcessBuilder(String logFilePath, String... args) {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    // Log to files — streams won't be tied to this JVM's lifecycle
    File logFile = new File(logFilePath);
    File errFile = new File(logFilePath.replace("update.log", "update-error.log"));
    processBuilder.redirectOutput(logFile);
    processBuilder.redirectError(errFile);
    return processBuilder;
  }

  @Override
  public void runRebootScript(String... args) throws IOException {
    runScriptInDifferentThread(getUpdateLogFile().getAbsolutePath(), args);
  }

  @java.lang.SuppressWarnings({"java:S6881"})
  private void runScriptInDifferentThread(String logFilePath, String... args) {
    Thread updateThread =
        new Thread(
            () -> {
              try {
                ProcessBuilder pb = getProcessBuilder(logFilePath, args);
                pb.start();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    updateThread.setDaemon(false);
    updateThread.setName("update-armadillo");
    updateThread.start();
  }
}
