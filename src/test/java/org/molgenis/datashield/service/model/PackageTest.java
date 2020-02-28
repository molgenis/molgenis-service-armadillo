package org.molgenis.datashield.service.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PackageTest {
  public static Package BASE =
      Package.builder()
          .setName("base")
          .setVersion("3.6.1")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  public static Package DESC =
      Package.builder()
          .setName("desc")
          .setVersion("1.2.0")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  @ParameterizedTest
  @ValueSource(strings = {"0abc", "abc.", "abö", "ab-cd", ".abc"})
  void testCheckInvalidNames(String name) {
    assertThrows(IllegalStateException.class, () -> Package.checkName(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "ABC", "aBc", "abc0", "ab.c"})
  void testCheckValidNames(String name) {
    Package.checkName(name);
  }
}
