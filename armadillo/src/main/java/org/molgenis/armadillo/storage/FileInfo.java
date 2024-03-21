package org.molgenis.armadillo.storage;

import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Objects;

public record FileInfo(
    String name, String size, String rows, String columns, String sourceLink, String[] variables) {
  public static FileInfo of(String name, String size) {
    return new FileInfo(name, size, null, null, null, new String[] {});
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FileInfo fileInfo = (FileInfo) o;

    if (!Objects.equals(name, fileInfo.name)) return false;
    if (!Objects.equals(size, fileInfo.size)) return false;
    if (!Objects.equals(rows, fileInfo.rows)) return false;
    if (!Objects.equals(columns, fileInfo.columns)) return false;
    if (!Iterables.elementsEqual(Arrays.asList(variables), Arrays.asList(fileInfo.variables)))
      return false;
    return Objects.equals(sourceLink, fileInfo.sourceLink);
  }
}
