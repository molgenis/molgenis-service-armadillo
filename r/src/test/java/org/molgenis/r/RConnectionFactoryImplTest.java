package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class RConnectionFactoryImplTest {

  EnvironmentConfigProps rConfigProperties = new EnvironmentConfigProps();
  @Mock RConnection rConnection;

  private RConnectionFactoryImpl rConnectionFactory;

  @BeforeEach
  void beforeEach() {
    rConfigProperties.setHost("host");
    rConfigProperties.setPort(123);
    rConnectionFactory = spy(new RConnectionFactoryImpl(rConfigProperties));
  }

  @Test
  void testGetNewConnection() throws RserveException, REXPMismatchException {
    doReturn(rConnection).when(rConnectionFactory).newConnection("host", 123);
    assertEquals(rConnection, rConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionCannotConnect() throws RserveException, REXPMismatchException {
    doThrow(new RserveException(rConnection, "Cannot connect"))
        .when(rConnectionFactory)
        .newConnection("host", 123);

    assertThrows(
        ConnectionCreationFailedException.class, () -> rConnectionFactory.retryCreateConnection());
  }
}
