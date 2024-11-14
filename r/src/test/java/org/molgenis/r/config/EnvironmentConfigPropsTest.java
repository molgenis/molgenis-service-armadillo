package org.molgenis.r.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class EnvironmentConfigPropsTest {
  EnvironmentConfigProps props = new EnvironmentConfigProps();

  @Test
  public void testHost() {
    props.setHost("notlocalhost");
    assertEquals("notlocalhost", props.getHost());
  }

  @Test
  public void testPort() {
    props.setPort(8085);
    assertEquals(8085, props.getPort());
  }

  @Test
  public void testName() {
    props.setName("bofke");
    assertEquals("bofke", props.getName());
  }

  @Test
  public void testImage() {
    props.setImage("datashield/rock-dolomite-xenon");
    assertEquals("datashield/rock-dolomite-xenon", props.getImage());
  }
}
