package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileStartStatus;
import org.molgenis.armadillo.profile.ProfileStatusService;

@ExtendWith(MockitoExtension.class)
class ProfileStartStatusControllerMockitoTest {

  @Mock private ProfileStatusService statusService;

  @InjectMocks private ProfileStartStatusController controller;

  @Test
  void getStatus_returnsServiceResult() {
    // arrange
    var expected = new ProfileStartStatus("donkey", "Installing profile", 22, 24);
    when(statusService.getStatus("donkey")).thenReturn(expected);

    // act
    ProfileStartStatus actual = controller.getStatus("donkey");

    // assert
    assertSame(expected, actual); // same instance passed through
    verify(statusService).getStatus("donkey");
    verifyNoMoreInteractions(statusService);
  }

  @Test
  void getStatus_unknown_returnsAllNulls() {
    // arrange
    var expected = new ProfileStartStatus(null, null, null, null);
    when(statusService.getStatus("missing")).thenReturn(expected);

    // act
    ProfileStartStatus actual = controller.getStatus("missing");

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
    var expected = new ProfileStartStatus("ShReK", "Profile installed", 24, 24);
    when(statusService.getStatus("ShReK")).thenReturn(expected);

    // act
    ProfileStartStatus actual = controller.getStatus("ShReK");

    // assert
    assertEquals("ShReK", actual.getProfileName());
    assertEquals("Profile installed", actual.getStatus());
    verify(statusService).getStatus("ShReK");
    verifyNoMoreInteractions(statusService);
  }
}
