package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.ContainerStartStatus;

class ContainerStatusServiceTest {

  @Test
  void getStatus_returnsAllNulls_whenProfileUnknown() {
    ContainerStatusService service = new ContainerStatusService();

    ContainerStartStatus result = service.getStatus("missing");

    assertAll(
        () -> assertNull(result.containerName()),
        () -> assertNull(result.status()),
        () -> assertNull(result.completedLayers()),
        () -> assertNull(result.totalLayers()));
  }

  @Test
  void updateStatus_storesValuesCorrectly() {
    ContainerStatusService service = new ContainerStatusService();

    service.updateStatus("donkey", "Installing container", 10, 24);
    ContainerStartStatus result = service.getStatus("donkey");

    assertAll(
        () -> assertEquals("donkey", result.containerName()),
        () -> assertEquals("Installing container", result.status()),
        () -> assertEquals(10, result.completedLayers()),
        () -> assertEquals(24, result.totalLayers()));
  }

  @Test
  void updateStatus_overwritesPreviousValue() {
    ContainerStatusService service = new ContainerStatusService();

    service.updateStatus("donkey", "Installing container", 5, 24);
    service.updateStatus("donkey", "Profile installed", 24, 24);
    ContainerStartStatus result = service.getStatus("donkey");

    assertAll(
        () -> assertEquals("donkey", result.containerName()),
        () -> assertEquals("Profile installed", result.status()),
        () -> assertEquals(24, result.completedLayers()),
        () -> assertEquals(24, result.totalLayers()));
  }

  @Test
  void updateStatus_handlesNullValues() {
    ContainerStatusService service = new ContainerStatusService();

    service.updateStatus("shrek", null, null, null);
    ContainerStartStatus result = service.getStatus("shrek");

    assertAll(
        () -> assertEquals("shrek", result.containerName()),
        () -> assertNull(result.status()),
        () -> assertNull(result.completedLayers()),
        () -> assertNull(result.totalLayers()));
  }

  @Test
  void concurrentUpdates_remainConsistent() throws Exception {
    ContainerStatusService service = new ContainerStatusService();
    ExecutorService executor = Executors.newFixedThreadPool(8);
    int n = 100;

    for (int i = 0; i < n; i++) {
      int idx = i;
      executor.submit(() -> service.updateStatus("p" + idx, "Installing", idx, n));
    }

    executor.shutdown();
    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

    for (int i = 0; i < n; i++) {
      final int idx = i;
      final int total = n;

      ContainerStartStatus s = service.getStatus("p" + idx);

      assertAll(
          () -> assertEquals("p" + idx, s.containerName()),
          () -> assertEquals("Installing", s.status()),
          () -> assertEquals(idx, s.completedLayers()),
          () -> assertEquals(total, s.totalLayers()));
    }
  }
}
