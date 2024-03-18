package org.molgenis.armadillo.metadata;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.armadillo.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class InsightService {
  public static final String AUDIT_FILE = "AUDIT_FILE";
  public static final String LOG_FILE = "LOG_FILE";

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

  /**
   * Map file_id to injected file paths.
   *
   * @param file_id one of injected names above.
   * @return File path or given file id.
   */
  public String getFileName(String file_id) {
    InsightServiceFiles c = InsightServiceFiles.getConstantByKey(file_id);
    if (!(c == null)) {
      return switch (file_id) {
        case LOG_FILE -> logFilePath;
        case AUDIT_FILE -> auditFilePath;
        default -> "Unregistered file name mapping: " + file_id;
      };
    }
    return file_id;
  }

  public String getDownloadName(String file_id) {
    String requestTime = getServerTime().replace(" ", "T").replace(":", "").replace("-", "");

    InsightServiceFiles c = InsightServiceFiles.getConstantByKey(file_id);
    if (!(c == null)) {
      return c.getDownloadName() + "-" + requestTime;
    }
    return file_id;
  }

  public FileDetails fileDetails(String file_id, int pageNum, int pageSize, String direction) {
    InsightServiceFiles insightServiceFiles = InsightServiceFiles.getConstantByKey(file_id);
    if (!(insightServiceFiles == null)) {
      String filePath = getFileName(file_id);
      String content = this.fileService.readLogFileBiz(filePath, pageNum, pageSize, direction);
      return FileDetails.create(
          insightServiceFiles.getKey(),
          insightServiceFiles.getDisplayName(),
          insightServiceFiles.getContentType(),
          content,
          getServerTime() + ": " + fileService.getFileSize(filePath),
          pageNum,
          pageSize);
    }
    return FileDetails.create(file_id, file_id, "text/plain", file_id, getServerTime(), -1, -1);
  }

  public FileInputStream downloadFile(String file_id) {
    return switch (file_id) {
      case LOG_FILE, AUDIT_FILE -> this.fileService.streamLogFile(getFileName(file_id));
      default -> (FileInputStream) FileInputStream.nullInputStream();
    };
  }
}

enum InsightServiceFiles {
  AUDIT_FILE("AUDIT_FILE", MediaType.APPLICATION_NDJSON_VALUE, "Audit file", "armadillo-audit"),
  LOG_FILE("LOG_FILE", MediaType.TEXT_PLAIN_VALUE, "Log file", "armadillo-log");

  private final String key;
  private final String contentType;
  private final String displayName;
  private final String downloadName;

  InsightServiceFiles(String key, String contentType, String displayName, String downloadName) {
    this.key = key;
    this.contentType = contentType;
    this.displayName = displayName;
    this.downloadName = downloadName;
  }

  public String getKey() {
    return key;
  }

  public String getContentType() {
    return contentType;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDownloadName() {
    return downloadName;
  }

  public static boolean hasKey(String key) {
    for (InsightServiceFiles constant : InsightServiceFiles.values()) {
      if (constant.getKey().equals(key)) {
        return true;
      }
    }
    return false;
  }

  public static InsightServiceFiles getConstantByKey(String key) {
    for (InsightServiceFiles constant : InsightServiceFiles.values()) {
      if (constant.getKey().equals(key)) {
        return constant;
      }
    }
    return null;
  }
}
