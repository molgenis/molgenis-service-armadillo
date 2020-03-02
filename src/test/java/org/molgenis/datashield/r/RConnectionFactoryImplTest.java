package org.molgenis.datashield.r;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class RConnectionFactoryImplTest {

  RConfigProperties rConfigProperties = new RConfigProperties();
  @Mock DataShieldOptions dataShieldOptions;
  @Mock RConnection rConnection;
  @Mock REXP sessionInfo;

  private RConnectionFactoryImpl rConnectionFactory;

  @BeforeEach
  void beforeEach() {
    rConfigProperties.setHost("host");
    rConfigProperties.setPort(123);
    rConnectionFactory = spy(new RConnectionFactoryImpl(rConfigProperties, dataShieldOptions));
  }

  @Test
  void testGetNewConnection() throws RserveException, REXPMismatchException {
    doReturn(rConnection).when(rConnectionFactory).newConnection("host", 123);
    when(dataShieldOptions.getValue()).thenReturn(ImmutableMap.of("a", "80.0"));
    when(rConnection.eval("options(a = 80.0)")).thenReturn(new REXPNull());
    when(rConnection.eval("capture.output(sessionInfo())")).thenReturn(sessionInfo);
    when(sessionInfo.asStrings()).thenReturn(new String[] {"session info", "multiple lines"});

    assertEquals(rConnection, rConnectionFactory.getNewConnection(false));
  }

  @Test
  void testGetNewConnectionCannotConnect() throws RserveException, REXPMismatchException {
    doThrow(new RserveException(rConnection, "Cannot connect"))
        .when(rConnectionFactory)
        .newConnection("host", 123);

    assertThrows(
        ConnectionCreationFailedException.class, () -> rConnectionFactory.getNewConnection(false));
  }
}
