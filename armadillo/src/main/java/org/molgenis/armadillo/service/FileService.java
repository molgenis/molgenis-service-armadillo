package org.molgenis.armadillo.service;

import java.io.*;
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
      return "Error reading log file on '" + logFilePath + "'";
    }
    return stringBuilder.toString();
  }
}
