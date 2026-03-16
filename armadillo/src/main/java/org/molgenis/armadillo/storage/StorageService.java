package org.molgenis.armadillo.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.molgenis.armadillo.model.ArmadilloColumnMetaData;
import org.springframework.http.MediaType;

public interface StorageService {
  public String getRootDir();

  public String getFilenameWithoutExtension(String filename);

  boolean objectExists(String bucket, String objectName);

  boolean bucketExists(String bucket);

  List<String> getUnavailableVariables(String bucketName, String objectName, String variables)
      throws IOException;

  void createBucketIfNotExists(String bucketName);

  void deleteBucket(String bucketName);

  List<String> listBuckets();

  void save(InputStream is, String bucketName, String objectName, MediaType mediaType);

  List<ObjectMetadata> listObjects(String bucketName);

  InputStream load(String bucketName, String objectName);

  FileInfo getInfo(String bucketName, String objectName);

  List<String> getVariables(String bucketName, String objectName);

  List<Map<String, String>> preview(
      String bucketName, String objectName, int rowLimit, int columnLimit);

  void delete(String bucketName, String objectName);

  Path getPathIfObjectExists(String bucketName, String objectName);

  static String getHumanReadableByteCount(long bytes) {
    long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
    if (absB < 1024) {
      return bytes + " B";
    }
    long value = absB;
    CharacterIterator ci = new StringCharacterIterator("KMGTPE");
    for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
      value >>= 10;
      ci.next();
    }
    value *= Long.signum(bytes);
    Locale.setDefault(Locale.US);
    return String.format("%.1f %cB", value / 1024.0, ci.current());
  }

  ArmadilloWorkspace getWorkSpace(InputStream is);

  Map<String, ArmadilloColumnMetaData> getMetadataFromTablePath(
      String bucketName, String objectName);
}
