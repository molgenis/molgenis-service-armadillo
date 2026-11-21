package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.InitialContainerConfig;
import org.molgenis.armadillo.metadata.UpdateSchedule;

class DatashieldInitialConfigBuilderTest {

  @Test
  void build_mapsInitialConfigFields() {
    InitialContainerConfig initial = new InitialContainerConfig();
    initial.setName("donkey");
    initial.setImage("datashield/armadillo-rserver:6.2.0");
    initial.setHost("localhost");
    initial.setPort(6311);
    initial.setAutoUpdate(true);
    initial.setUpdateSchedule(new UpdateSchedule("weekly", "Sunday", "01:00"));
    initial.setPackageWhitelist(Set.of("dsBase"));
    initial.setFunctionBlacklist(Set.of("list"));
    initial.setOptions(Map.of("opt", "value"));

    DatashieldInitialConfigBuilder builder = new DatashieldInitialConfigBuilder();
    ContainerConfig config = builder.build(initial);
    DatashieldContainerConfig datashield = (DatashieldContainerConfig) config;

    assertEquals("donkey", datashield.getName());
    assertEquals("datashield/armadillo-rserver:6.2.0", datashield.getImage());
    assertEquals("localhost", datashield.getHost());
    assertEquals(6311, datashield.getPort());
    assertEquals(Boolean.TRUE, datashield.getAutoUpdate());
    assertEquals(initial.getUpdateSchedule(), datashield.getUpdateSchedule());
    assertEquals(Set.of("dsBase"), datashield.getPackageWhitelist());
    assertEquals(Set.of("list"), datashield.getFunctionBlacklist());
    assertEquals(Map.of("opt", "value"), datashield.getDatashieldROptions());
    assertNull(datashield.getLastImageId());
    assertNull(datashield.getImageSize());
    assertNull(datashield.getInstallDate());
    assertNull(datashield.getVersionId());
    assertNull(datashield.getCreationDate());
    assertEquals(java.util.List.of(), datashield.getDockerArgs());
    assertEquals(java.util.Map.of(), datashield.getDockerOptions());
  }
}
