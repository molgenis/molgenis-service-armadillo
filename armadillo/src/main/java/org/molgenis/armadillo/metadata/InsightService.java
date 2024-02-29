package org.molgenis.armadillo.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }

  @Value("${stdout.log.path:./logs/armadillo.log}")
  private String logFilePath;

  @Value("${audit.log.path:./logs/audit.log}")
  private String auditFilePath;

  public String getFileName(String file_id) {
    return switch (file_id) {
      case LOG_FILE -> LOG_FILE_NAME;
      case AUDIT_FILE -> AUDIT_FILE_NAME;
      default -> file_id;
    };
  }

  public FileDetails fileDetails(String file_id, int pageNum, int pageSize) {
    return switch (file_id) {
      case LOG_FILE -> FileDetails.create(
          LOG_FILE,
          LOG_FILE_DISPLAY_NAME,
          this.fileService.readLogFile(logFilePath, pageNum, pageSize),
          getServerTime() + ": " + fileService.getFileSize(logFilePath),
          pageNum,
          pageSize);
      case AUDIT_FILE -> FileDetails.create(
          AUDIT_FILE,
          AUDIT_FILE_DISPLAY_NAME,
          this.fileService.readLogFile(auditFilePath, pageNum, pageSize),
          getServerTime() + ": " + fileService.getFileSize(auditFilePath),
          pageNum,
          pageSize);
      default -> FileDetails.create(file_id, file_id, file_id, getServerTime(), -1, -1);
    };
  }

  public Stream<String> downloadFile(String file_id) {
    return switch (file_id) {
      case LOG_FILE -> this.fileService.streamLogFile(logFilePath);
      case AUDIT_FILE -> this.fileService.streamLogFile(auditFilePath);
      default -> Stream.empty();
    };
  }
}
