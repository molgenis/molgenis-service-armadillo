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
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.connection.datashield.RConnectionFactory;
import org.molgenis.connection.datashield.RServerConnection;
import org.molgenis.connection.datashield.RServerException;
import org.molgenis.connection.datashield.rock.RockResult;
import org.molgenis.connection.exceptions.ConnectionCreationFailedException;
import org.molgenis.connection.service.PackageService;
import org.rosuda.REngine.REXPNull;

@ExtendWith(MockitoExtension.class)
class ArmadilloConnectionFactoryImplTest {

  @Mock DataShieldOptions dataShieldOptions;
  @Mock RConnectionFactory rConnectionFactory;
  @Mock PackageService packageService;
  @Mock RServerConnection rConnection;
  @Mock DatashieldContainerConfig datashieldContainerConfig;

  private ArmadilloConnectionFactoryImpl armadilloConnectionFactory;

  @BeforeEach
  void beforeEach() {

    armadilloConnectionFactory =
        new ArmadilloConnectionFactoryImpl(
            packageService, datashieldContainerConfig, dataShieldOptions, rConnectionFactory);
  }

  @Test
  void testGetNewConnection() throws RServerException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue(rConnectionFactory.tryCreateConnection()))
        .thenReturn(ImmutableMap.of("a", "80.0"));
    when(rConnection.eval("base::options(a = 80.0)")).thenReturn(new RockResult(new REXPNull()));

    assertEquals(rConnection, armadilloConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionWithStringOption() throws RServerException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue(rConnectionFactory.tryCreateConnection()))
        .thenReturn(ImmutableMap.of("b", "permissive"));
    when(rConnection.eval("base::options(b = \"permissive\")"))
        .thenReturn(new RockResult(new REXPNull()));

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
