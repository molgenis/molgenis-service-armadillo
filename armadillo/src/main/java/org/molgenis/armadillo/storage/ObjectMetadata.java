package org.molgenis.armadillo.storage;

import io.minio.messages.Item;
import java.nio.file.Path;
import java.util.Date;

/**
 * @param name The fully qualified name of the object (e.g. core/nonrep.parquet)
 * @param lastModified The date & time the object was last modified
 * @param size The size of the object in bytes
 */
public record ObjectMetadata(String name, Date lastModified, long size) {

  /**
   * @param projectPath The path to the project folder (e.g. data/lifecycle)
   * @param objectPath The path to the object (e.g. data/lifecycle/core/nonrep.parquet)
   */
  public static ObjectMetadata of(Path projectPath, Path objectPath) {
    var name = projectPath.relativize(objectPath).toString();
    var file = objectPath.toFile();
    return new ObjectMetadata(name, new Date(file.lastModified()), file.length());
  }

  public static ObjectMetadata of(Item item) {
    return new ObjectMetadata(item.objectName(), item.lastModified(), item.objectSize());
  }
}
