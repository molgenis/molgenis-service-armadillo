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
  public static final String AUDIT_FILE = "AUDIT_FILE";
  public static final String AUDIT_FILE_NAME = "armadillo-audit.log";
  public static final String AUDIT_FILE_DISPLAY_NAME = "Audit file";
  public static final String LOG_FILE = "LOG_FILE";
  public static final String LOG_FILE_NAME = "armadillo-log.log";
  public static final String LOG_FILE_DISPLAY_NAME = "Log file";

  private final FileService fileService;

  public InsightService(FileService fileService) {
    this.fileService = fileService;
  }

  public List<FileInfo> filesInfo() {
    ArrayList<FileInfo> list = new ArrayList<>();
    list.add(FileInfo.create(AUDIT_FILE, AUDIT_FILE_DISPLAY_NAME));
    list.add(FileInfo.create(LOG_FILE, LOG_FILE_DISPLAY_NAME));
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
      case LOG_FILE -> FileDetails.create(
          LOG_FILE,
          LOG_FILE_DISPLAY_NAME,
          this.fileService.readLogFile(logFilePath),
          getServerTime());
      case AUDIT_FILE -> FileDetails.create(
          AUDIT_FILE,
          AUDIT_FILE_DISPLAY_NAME,
          this.fileService.readLogFile(auditFilePath),
          getServerTime());
      default -> FileDetails.create(file_id, file_id, file_id, getServerTime());
    };
  }

  public FileDetails downloadFile(String file_id) {
    return switch (file_id) {
      case LOG_FILE -> FileDetails.create(
          LOG_FILE, LOG_FILE_NAME, this.fileService.readLogFile(logFilePath), getServerTime());
      case AUDIT_FILE -> FileDetails.create(
          AUDIT_FILE,
          AUDIT_FILE_NAME,
          this.fileService.readLogFile(auditFilePath),
          getServerTime());
      default -> FileDetails.create(file_id, file_id, file_id, getServerTime());
    };
  }
}
