package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.rserve.RserveResult;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.REXPNull;

@ExtendWith(MockitoExtension.class)
class ArmadilloConnectionFactoryImplTest {

  @Mock DataShieldOptions dataShieldOptions;
  @Mock RConnectionFactory rConnectionFactory;
  @Mock PackageService packageService;
  @Mock RServerConnection rConnection;
  @Mock ProfileConfig profileConfig;

  private ArmadilloConnectionFactoryImpl armadilloConnectionFactory;

  @BeforeEach
  void beforeEach() {

    armadilloConnectionFactory =
        new ArmadilloConnectionFactoryImpl(
            packageService, profileConfig, dataShieldOptions, rConnectionFactory);
  }

  @Test
  void testGetNewConnection() throws RServerException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue(rConnectionFactory.tryCreateConnection()))
        .thenReturn(ImmutableMap.of("a", "80.0"));
    when(rConnection.eval("base::options(a = 80.0)")).thenReturn(new RserveResult(new REXPNull()));

    assertEquals(rConnection, armadilloConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionWithStringOption() throws RServerException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue(rConnectionFactory.tryCreateConnection()))
        .thenReturn(ImmutableMap.of("b", "permissive"));
    when(rConnection.eval("base::options(b = \"permissive\")"))
        .thenReturn(new RserveResult(new REXPNull()));

    assertEquals(rConnection, armadilloConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionCannotConnect() {
    doThrow(new ConnectionCreationFailedException("Mislukt"))
        .when(rConnectionFactory)
        .tryCreateConnection();

    assertThrows(
        ConnectionCreationFailedException.class, () -> rConnectionFactory.tryCreateConnection());
  }
}
