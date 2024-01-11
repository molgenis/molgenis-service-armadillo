package org.molgenis.armadillo.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class InsightService {

  private final FileService fileService;

  public InsightService(FileService fileService) {
    this.fileService = fileService;
  }

  public List<FileInfo> filesInfo() {
    ArrayList<FileInfo> list = new ArrayList<>();
    list.add(FileInfo.create("AUDIT_FILE", "Audit file"));
    list.add(FileInfo.create("LOG_FILE", "Log file"));
    list.add(FileInfo.create("LOGGER_INFO", "Log settings"));
    list.add(FileInfo.create("CONFIG", "Current configuration"));
    return list;
  }

  private String getServerTime() {
    LocalDateTime now = LocalDateTime.now();
    System.out.println("Current DateTime: " + now);

    // To print in a particular format
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }

  @Value("${stdout.log.path:./logs/armadillo.log}")
  private String logFilePath;

  @Value("${audit.log.path:./logs/audit.log}")
  private String auditFilePath;

  public FileDetails fileDetails(String file_id) {
    return switch (file_id) {
      case "LOGGER_INFO" -> FileDetails.create(
          file_id, "Logger info", this.fileService.DumpLoggers(), getServerTime());
      case "LOG_FILE" -> FileDetails.create(
          file_id, "Log file", this.fileService.readLogFile(logFilePath), getServerTime());
      case "AUDIT_FILE" -> FileDetails.create(
          file_id, "Audit file", this.fileService.readLogFile(auditFilePath), getServerTime());
      case "CONFIG" -> FileDetails.create(file_id, "Configuration", "tbd", getServerTime());
      default -> FileDetails.create(file_id, file_id, file_id, getServerTime());
    };
  }

  public FileDetails downloadFile(String file_id) {
    return switch (file_id) {
      case "LOGGER_INFO" -> FileDetails.create(
          file_id, "Logger info", this.fileService.DumpLoggers(), getServerTime());
      case "LOG_FILE" -> FileDetails.create(
          file_id, "Log file", this.fileService.readLogFile(logFilePath), getServerTime());
      case "AUDIT_FILE" -> FileDetails.create(
          file_id, "Audit file", this.fileService.readLogFile(auditFilePath), getServerTime());
      case "CONFIG" -> FileDetails.create(file_id, "Configuration", "tbd", getServerTime());
      default -> FileDetails.create(file_id, file_id, file_id, getServerTime());
    };
  }
}
