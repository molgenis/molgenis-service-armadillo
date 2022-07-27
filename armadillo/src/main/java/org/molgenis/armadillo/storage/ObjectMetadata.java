package org.molgenis.armadillo.storage;

import io.minio.messages.Item;
import java.io.File;
import java.util.Date;

public class ObjectMetadata {
  private String name;
  private Date lastModified;
  private long size;

  public ObjectMetadata(File file) {
    this.name = file.getName();
    this.lastModified = new Date(file.lastModified());
    this.size = file.length();
  }

  public ObjectMetadata(Item item) {
    this.name = item.objectName();
    this.lastModified = item.lastModified();
    this.size = item.objectSize();
  }

  public String getName() {
    return name;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public long getSize() {
    return size;
  }
}
