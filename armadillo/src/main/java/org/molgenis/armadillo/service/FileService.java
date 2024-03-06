package org.molgenis.armadillo.service;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.molgenis.armadillo.metadata.TextBlockReader;
import org.molgenis.armadillo.profile.annotation.ProfileScope;
import org.springframework.stereotype.Service;

@Service
@ProfileScope
public class FileService {

  public static int pageNumFromDirection(int pageNum, String direction) {
    if (direction.equals("end")) {
      pageNum = -pageNum - 1;
    }
    return pageNum;
  }

  /**
   * Read page of lines from given pageNum and pageSize from direction.
   *
   * <p>The pageSize and pageNum for direction asked for can fall out of file lines range.
   *
   * @param logFilePath file to read from
   * @param pageNum page num depends on pageSize
   * @param pageSize number of lines to read
   * @return lines falling in the asked frame
   */
  public String readLogFile(String logFilePath, int pageNum, int pageSize, String direction) {
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    pageNum = pageNumFromDirection(pageNum, direction);

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

  public String readLogFileBiz(String logFilePath, int pageNum, int pageSize, String direction) {
    long fileSize = getFileSize(logFilePath);

    // file does not exist OR trying to read past file size
    if (fileSize == -1 || (long) pageNum * pageSize > fileSize) {
      return "";
    }

    // From end makes negative pageNum ie -1 means 1 pageSize from end
    pageNum = pageNumFromDirection(pageNum, direction);

    try {
      TextBlockReader textBlockReader = new TextBlockReader(logFilePath);
      long startPosition = (long) pageNum * pageSize + (direction.equals("end") ? fileSize : 0);
      long endPosition = startPosition + pageSize;

      if (startPosition > fileSize) return "";

      if (startPosition < 0) startPosition = 0;
      if (endPosition > fileSize) endPosition = fileSize;

      if (endPosition < startPosition) return "";

      try {
        BufferedReader bufferedReader = textBlockReader.readBlock(startPosition, endPosition);
        return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
      } catch (IOException e) {
        e.printStackTrace();
        return "";
      }
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  public FileInputStream streamLogFile(String logFilePath) {
    try {
      Path path = Path.of(logFilePath);
      return new FileInputStream(path.toFile());
    } catch (IOException e) {
      e.printStackTrace();
      return (FileInputStream) FileInputStream.nullInputStream();
    }
  }

  public long getFileSize(String filePath) {
    try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath))) {
      return fileChannel.size();
    } catch (IOException e) {
      return -1;
    }
  }
}
