package org.molgenis.armadillo.metadata;

import org.springframework.http.MediaType;

public enum InsightServiceFiles {
  AUDIT_FILE("AUDIT_FILE", MediaType.APPLICATION_NDJSON_VALUE, "armadillo-audit.log", "Audit file"),
  LOG_FILE("LOG_FILE", MediaType.TEXT_PLAIN_VALUE, "armadillo-log.log", "Log file");

  private final String key;
  private final String contentType;
  private final String file_name;
  private final String displayName;

  InsightServiceFiles(String key, String contentType, String file_name, String displayName) {
    this.key = key;
    this.contentType = contentType;
    this.file_name = file_name;
    this.displayName = displayName;
  }

  public String getKey() {
    return key;
  }

  public String getContentType() {
    return contentType;
  }

  public String getFileName() {
    return file_name;
  }

  public String getDisplayName() {
    return displayName;
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
