package org.molgenis.armadillo.storage;

import io.minio.messages.Item;
import java.io.File;
import java.util.Date;

/**
 * @param name The fully qualified name of the object (e.g. lifecycle/core/nonrep)
 * @param lastModified The date & time the object was last modified
 * @param size The size of the object in bytes
 */
public record ObjectMetadata(String name, Date lastModified, long size) {
  public static ObjectMetadata of(File file) {
    return new ObjectMetadata(file.getName(), new Date(file.lastModified()), file.length());
  }

  public static ObjectMetadata of(Item item) {
    return new ObjectMetadata(item.objectName(), item.lastModified(), item.objectSize());
  }
}
