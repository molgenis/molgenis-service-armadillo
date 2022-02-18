package org.molgenis.armadillo.service;

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
import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class ArmadilloConnectionFactoryImplTest {

  @Mock DataShieldOptions dataShieldOptions;
  @Mock RConnectionFactory rConnectionFactory;
  @Mock PackageService packageService;
  @Mock RConnection rConnection;
  @Mock ProfileConfigProps profileConfigProps;

  private ArmadilloConnectionFactoryImpl armadilloConnectionFactory;

  @BeforeEach
  void beforeEach() {

    armadilloConnectionFactory =
        new ArmadilloConnectionFactoryImpl(
            packageService, profileConfigProps, dataShieldOptions, rConnectionFactory);
  }

  @Test
  void testGetNewConnection() throws RserveException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue()).thenReturn(ImmutableMap.of("a", "80.0"));
    when(rConnection.eval("base::options(a = 80.0)")).thenReturn(new REXPNull());

    assertEquals(rConnection, armadilloConnectionFactory.createConnection());
  }

  @Test
  void testGetNewConnectionWithStringOption() throws RserveException {
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();
    when(dataShieldOptions.getValue()).thenReturn(ImmutableMap.of("b", "permissive"));
    when(rConnection.eval("base::options(b = \"permissive\")")).thenReturn(new REXPNull());

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
