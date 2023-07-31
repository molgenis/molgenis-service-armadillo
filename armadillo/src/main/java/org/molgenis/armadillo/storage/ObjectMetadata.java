package org.molgenis.armadillo.storage;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @param name The fully qualified name of the object (e.g. core/nonrep.parquet)
 * @param lastModified The date & time the object was last modified
 * @param size The size of the object in bytes
 */
public record ObjectMetadata(String name, ZonedDateTime lastModified, long size) {

  /**
   * @param projectPath The path to the project folder (e.g. data/lifecycle)
   * @param objectPath The path to the object (e.g. data/lifecycle/core/nonrep.parquet)
   */
  public static ObjectMetadata of(Path projectPath, Path objectPath) {
    var name = projectPath.relativize(objectPath).toString();
    var file = objectPath.toFile();
    var instant = Instant.ofEpochMilli(file.lastModified());
    return new ObjectMetadata(
        name, ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()), file.length());
  }
}
