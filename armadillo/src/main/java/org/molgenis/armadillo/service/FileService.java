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

  /**
   * Read page of lines from given pageNum and pageSize.
   *
   * <p>The pageSize and pageNum asked for can fall out of file lines range.
   *
   * @param logFilePath file to read from
   * @param pageNum page num depends on pageSize
   * @param pageSize number of lines to read
   * @return lines falling in the asked frame
   */
  public String readLogFile(String logFilePath, int pageNum, int pageSize, String direction) {
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    if (direction.equals("end")) {
      pageNum = -pageNum - 1;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
      long totalLines = new BufferedReader(new FileReader(logFilePath)).lines().count();
      long startLine;
      long endLine;

      if (pageSize == 0) {
        // Read the entire file
        startLine = 0;
        endLine = Long.MAX_VALUE;
      } else {
        if (pageNum < 0) {
          // NOTE: page is negative
          startLine = totalLines + (long) pageNum * pageSize;
        } else {
          startLine = (long) pageNum * pageSize;
        }

        endLine = startLine + pageSize;

        // Restrict frame bounds
        startLine = Math.max(0, startLine);
        endLine = Math.min(endLine, totalLines);
      }

      long lineRead = 0;
      while ((line = reader.readLine()) != null) {
        if (startLine <= lineRead && lineRead < endLine) {
          stringBuilder.append(line).append("\n");
        }
        if (endLine < lineRead) {
          break;
        }
        lineRead += 1;
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

  public String getFileSize(String filePath) {
    try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath))) {
      return String.valueOf(fileChannel.size());
    } catch (IOException e) {
      return "-1";
    }
  }
}
