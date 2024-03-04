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
    list.add(
        FileInfo.create(
            InsightServiceFiles.AUDIT_FILE.getKey(),
            InsightServiceFiles.AUDIT_FILE.getDisplayName()));
    list.add(
        FileInfo.create(
            InsightServiceFiles.LOG_FILE.getKey(), InsightServiceFiles.LOG_FILE.getDisplayName()));
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
    InsightServiceFiles c = InsightServiceFiles.getConstantByKey(file_id);
    if (!(c == null)) {
      return c.getFileName();
    }
    return file_id;
  }

  public FileDetails fileDetails(String file_id, int pageNum, int pageSize, String direction) {
    InsightServiceFiles c = InsightServiceFiles.getConstantByKey(file_id);
    // FIXME: can we move this into InsightServiceFiles?
    if (!(c == null)) {
      String filePath = auditFilePath;
      if (file_id.equals("LOG_FILE")) {
        filePath = logFilePath;
      }
      String content = this.fileService.readLogFile(filePath, pageNum, pageSize, direction);
      return FileDetails.create(
          c.getKey(),
          c.getDisplayName(),
          c.getContentType(),
          content,
          getServerTime() + ": " + fileService.getFileSize(auditFilePath),
          pageNum,
          pageSize);
    }
    return FileDetails.create(file_id, file_id, "text/plain", file_id, getServerTime(), -1, -1);
  }

  public Stream<String> downloadFile(String file_id) {
    return switch (file_id) {
      case LOG_FILE -> this.fileService.streamLogFile(logFilePath);
      case AUDIT_FILE -> this.fileService.streamLogFile(auditFilePath);
      default -> Stream.empty();
    };
  }
}
