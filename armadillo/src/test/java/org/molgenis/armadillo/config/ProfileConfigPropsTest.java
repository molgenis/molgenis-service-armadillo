package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Maps;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileConfigPropsTest {

  private ProfileConfigProps props;

  @BeforeEach
  void before() {
    props = new ProfileConfigProps();
  }

  @Test
  void testOptions() {
    Map<String, String> options =
        new HashMap<>() {
          {
            put("key1", "value1");
            put("key2", "value2");
          }
        };
    props.setOptions(options);
    Map<String, String> actual = props.getOptions();
    assertTrue(Maps.difference(options, actual).areEqual());
  }

  @Test
  void testWhitelist() {
    String pkg1 = "beautiful-pkg";
    String pkg2 = "ugly-pkg";
    props.addToWhitelist(pkg1);
    props.addToWhitelist(pkg2);
    Set<String> expected = new HashSet<>(Arrays.asList(pkg1, pkg2));
    Set<String> actual = props.getWhitelist();
    assertEquals(actual, expected);
  }

  @Test
  void testName() {
    String name = "test properties";
    props.setName(name);
    String actual = props.getName();
    assertEquals(actual, name);
  }
}
