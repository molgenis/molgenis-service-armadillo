package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import org.junit.jupiter.api.Test;

class ContainerStatusTest {

  @Test
  void ofContainerState_running() {
    ContainerState state = mock(ContainerState.class);
    when(state.getRunning()).thenReturn(true);

    assertEquals(ContainerStatus.RUNNING, ContainerStatus.of(state));
  }

  @Test
  void ofContainerState_notRunning() {
    ContainerState state = mock(ContainerState.class);
    when(state.getRunning()).thenReturn(false);

    assertEquals(ContainerStatus.NOT_RUNNING, ContainerStatus.of(state));
  }

  @Test
  void ofString_running() {
    assertEquals(ContainerStatus.RUNNING, ContainerStatus.of("running"));
  }

  @Test
  void ofString_notRunning() {
    assertEquals(ContainerStatus.NOT_RUNNING, ContainerStatus.of("exited"));
  }
}
