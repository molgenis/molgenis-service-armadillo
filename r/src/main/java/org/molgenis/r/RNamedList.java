package org.molgenis.r;

import java.util.List;
import java.util.Map;

/**
 * R named list implemented as a read-only map.
 *
 * @param <T>
 */
public interface RNamedList<T> extends Map<String, T> {

  List<String> getNames();

  default T put(String s, T value) {
    throw new IllegalArgumentException("Operation not available");
  }

  default T remove(Object o) {
    throw new IllegalArgumentException("Operation not available");
  }

  default void putAll(Map<? extends String, ? extends T> map) {
    throw new IllegalArgumentException("Operation not available");
  }

  default void clear() {
    throw new IllegalArgumentException("Operation not available");
  }

  List<Map<String, Object>> asRows();
}
