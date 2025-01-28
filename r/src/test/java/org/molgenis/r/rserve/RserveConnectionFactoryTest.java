package org.molgenis.r.rserve;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.molgenis.r.config.EnvironmentConfigProps;

class RserveConnectionFactoryTest {

  @Mock private EnvironmentConfigProps environmentConfigProps;

  private RserveConnectionFactory connectionFactory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(environmentConfigProps.getHost()).thenReturn("localhost");
    when(environmentConfigProps.getPort()).thenReturn(6311);
    connectionFactory = new RserveConnectionFactory(environmentConfigProps);
  }

  @Test
  void testToString() {
    // Arrange
    String expectedString = "RConnectionFactoryImpl{environment=TestEnvironment}";
    when(environmentConfigProps.getName()).thenReturn("TestEnvironment");

    // Act
    String result = connectionFactory.toString();

    // Assert
    assertEquals(expectedString, result);
  }
}
