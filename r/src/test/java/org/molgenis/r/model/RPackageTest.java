package org.molgenis.r.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RPackageTest {

  @ParameterizedTest
  @ValueSource(strings = {"0abc", "abc.", "abö", "ab-cd", ".abc"})
  void testCheckInvalidNames(String name) {
    assertThrows(IllegalStateException.class, () -> RPackage.checkName(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "ABC", "aBc", "abc0", "ab.c"})
  void testCheckValidNames(String name) {
    RPackage.checkName(name);
  }
}
