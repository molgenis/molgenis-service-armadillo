package org.molgenis.armadillo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.molgenis.armadillo.profile.annotation.ProfileScope;
import org.springframework.stereotype.Service;

@Service
@ProfileScope
public class FileService {

  public String readLogFile(String logFilePath, int page, int pageSize) {
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

  public String readLogFileBiz(String logFilePath, int page, int pageSize) {
    StringBuilder stringBuilder = new StringBuilder();
    String line;

    try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
      long totalLines = new BufferedReader(new FileReader(logFilePath)).lines().count();
      long startLine;
      long endLine;

      if (pageSize == 0) {
        // Read the entire file
        startLine = 0;
        endLine = Long.MAX_VALUE;
      } else {
        if (page < 0) {
          // NOTE: page is negative
          startLine = totalLines + (long) page * pageSize;
        } else {
          startLine = (long) page * pageSize;
        }

        endLine = startLine + pageSize;

        // Restrict frame bounds
        startLine = Math.max(0, startLine);
        endLine = Math.min(endLine, totalLines);
      }

      long lineRead = 0;
      while ((line = reader.readLine()) != null) {
        lineRead += 1;
        if (startLine <= lineRead && lineRead < endLine) {
          stringBuilder.append(lineRead).append(": ").append(line).append("\n");
        }
        if (endLine < lineRead) {
          break;
        }
      }
    } catch (IOException e) {
      return "Error reading log file on '" + logFilePath + "'";
    }

    return stringBuilder.toString();
  }

  public Stream<String> streamLogFile(String logFilePath) {
    try {
      Path path = Path.of(logFilePath);
      return Files.lines(path);
    } catch (IOException e) {
      e.printStackTrace();
      return Stream.empty();
    }
  }

  public String getFileSize(String file_path) {
    try (FileChannel fileChannel = FileChannel.open(Paths.get(file_path))) {
      return String.valueOf(fileChannel.size());
    } catch (IOException e) {
      return "-1";
    }
  }
}
