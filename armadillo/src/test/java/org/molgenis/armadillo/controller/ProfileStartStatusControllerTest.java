package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.container.ContainerStatusService;
import org.molgenis.armadillo.metadata.ContainerStartStatus;

@ExtendWith(MockitoExtension.class)
class ContainerStartStatusControllerMockitoTest {

  @Mock private ContainerStatusService statusService;

  @InjectMocks private ContainerStartStatusController controller;

  @Test
  void getStatus_returnsServiceResult() {
    // arrange
    var expected = new ContainerStartStatus("donkey", "Installing container", 22, 24);
    when(statusService.getStatus("donkey")).thenReturn(expected);

    // act
    ContainerStartStatus actual = controller.getStatus("donkey");

    // assert
    assertSame(expected, actual); // same instance passed through
    verify(statusService).getStatus("donkey");
    verifyNoMoreInteractions(statusService);
  }

  @Test
  void getStatus_unknown_returnsAllNulls() {
    // arrange
    var expected = new ContainerStartStatus(null, null, null, null);
    when(statusService.getStatus("missing")).thenReturn(expected);

    // act
    ContainerStartStatus actual = controller.getStatus("missing");

    // assert
    assertNotNull(actual);
    assertNull(actual.getProfileName());
    assertNull(actual.getStatus());
    assertNull(actual.getCompletedLayers());
    assertNull(actual.getTotalLayers());
    verify(statusService).getStatus("missing");
    verifyNoMoreInteractions(statusService);
  }

  @Test
  void pathVariable_isPassedThroughUnchanged() {
    // arrange
    var expected = new ContainerStartStatus("ShReK", "Profile installed", 24, 24);
    when(statusService.getStatus("ShReK")).thenReturn(expected);

    // act
    ContainerStartStatus actual = controller.getStatus("ShReK");

    // assert
    assertEquals("ShReK", actual.getProfileName());
    assertEquals("Profile installed", actual.getStatus());
    verify(statusService).getStatus("ShReK");
    verifyNoMoreInteractions(statusService);
  }
}
