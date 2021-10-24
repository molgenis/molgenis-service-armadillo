package org.molgenis.armadillo.info;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.config.RServeConfig;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class RProcessEndpointTest {
  @Mock private EnvironmentConfigProps environment1;
  @Mock private EnvironmentConfigProps environment2;
  @Mock private ProcessService processService;
  @Mock private RServeConfig rServeConfig;
  @Mock private RConnection connection;

  @Test
  void testDoWithConnection() {
    var endpoint =
        new RProcessEndpoint(processService, rServeConfig) {
          EnvironmentConfigProps selectedEnvironment = null;

          @Override
          RConnection connect(EnvironmentConfigProps environment) {
            selectedEnvironment = environment;
            return connection;
          }
        };

    when(rServeConfig.getEnvironments()).thenReturn(List.of(environment1, environment2));
    when(environment1.getName()).thenReturn("kick");
    when(environment2.getName()).thenReturn("windsock");

    assertSame(connection, endpoint.doWithConnection("windsock", connection -> connection));
    assertSame(environment2, endpoint.selectedEnvironment);
  }
}
