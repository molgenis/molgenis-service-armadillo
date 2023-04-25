package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.rserve.RserveConnection;
import org.molgenis.r.rserve.RserveConnectionFactoryImpl;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class RserveConnectionFactoryImplTest {

  EnvironmentConfigProps rConfigProperties = new EnvironmentConfigProps();
  @Mock RserveConnection rConnection;

  private RserveConnectionFactoryImpl rConnectionFactory;

  @BeforeEach
  void beforeEach() {
    rConfigProperties.setHost("host");
    rConfigProperties.setPort(123);
    rConnectionFactory = spy(new RserveConnectionFactoryImpl(rConfigProperties));
  }

  @Test
  void testGetNewConnectionCannotConnect() throws RserveException {
    doThrow(new RserveException((rConnection).getConnection(), "Cannot connect"))
        .when(rConnectionFactory)
        .newConnection("host", 123);

    assertThrows(
        ConnectionCreationFailedException.class, () -> rConnectionFactory.tryCreateConnection());
  }
}
