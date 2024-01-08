package org.molgenis.armadillo.service;

import java.io.*;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.molgenis.armadillo.profile.annotation.ProfileScope;
import org.springframework.stereotype.Service;

@Service
@ProfileScope
public class FileService {

  public String readLogFile(String logFilePath) {
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return "Error reading log file";
    }
    return stringBuilder.toString();
  }

  public String DumpLoggers() {
    LogManager manager = LogManager.getLogManager();
    Enumeration<String> loggerNames = manager.getLoggerNames();

    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);

    while (loggerNames.hasMoreElements()) {
      String loggerName = loggerNames.nextElement();
      Logger logger = manager.getLogger(loggerName);
      printWriter.println("Logger Name: " + loggerName);
      Handler[] handlers = logger.getHandlers();
      for (Handler handler : handlers) {
        printWriter.println("Handler: " + handler.getClass() + ": " + handler.getLevel());
      }
    }
    printWriter.close();

    return stringWriter.toString();
  }
}
