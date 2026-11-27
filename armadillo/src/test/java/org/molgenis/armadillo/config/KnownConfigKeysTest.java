package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KnownConfigKeysTest {

  private final KnownConfigKeys known = KnownConfigKeys.loadFromClasspath();

  @Test
  void ignoresSpellingStyleOfKey() {
    assertTrue(known.contains("armadillo.containerPrefix"));
  }

  @Test
  void acceptsAnyKeyBeneathAMap() {
    assertTrue(known.contains("logging.level.org.molgenis"));
  }

  @Test
  void acceptsAnyKeyBeneathAPropertiesEntry() {
    assertTrue(known.contains("spring.datasource.hikari.data-source-properties.cachePrepStmts"));
  }

  @Test
  void rejectsNamedChildOfAList() {
    assertFalse(known.contains("spring.security.user.roles.bogus"));
  }

  @Test
  void rejectsKeysSpringHasRemoved() {
    assertFalse(known.contains("spring.resources.add-mappings"));
  }
}
