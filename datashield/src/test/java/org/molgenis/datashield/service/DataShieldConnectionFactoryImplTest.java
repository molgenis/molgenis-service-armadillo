package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.DataShieldOptions;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class DataShieldConnectionFactoryImplTest {

  @Mock DataShieldOptions dataShieldOptions;
  @Mock RConnectionFactory rConnectionFactory;
  @Mock RConnection rConnection;

  private DataShieldConnectionFactoryImpl dataShieldConnectionFactory;

  @BeforeEach
  void beforeEach() {
    dataShieldConnectionFactory =
        new DataShieldConnectionFactoryImpl(dataShieldOptions, rConnectionFactory);
  }

  @Test
  void testGetNewConnection() throws RserveException {
    doReturn(rConnection).when(rConnectionFactory).createConnection();
    when(dataShieldOptions.getValue()).thenReturn(ImmutableMap.of("a", "80.0"));
    when(rConnection.eval("library(dsBase)")).thenReturn(new REXPNull());
    when(rConnection.eval("options(a = 80.0)")).thenReturn(new REXPNull());

    assertEquals(rConnection, dataShieldConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionCannotConnect() {
    doThrow(new ConnectionCreationFailedException("Mislukt"))
        .when(rConnectionFactory)
        .createConnection();

    assertThrows(
        ConnectionCreationFailedException.class, () -> rConnectionFactory.createConnection());
  }
}
