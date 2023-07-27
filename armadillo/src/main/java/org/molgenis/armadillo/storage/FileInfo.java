package org.molgenis.armadillo.storage;

public record FileInfo(String name, String size, String rows, String columns) {
  public static FileInfo of(String name, String size) {
    return new FileInfo(name, size, null, null);
  }
}
