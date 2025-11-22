package org.molgenis.armadillo.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.service.ProcessService;

@ExtendWith(MockitoExtension.class)
class RProcessEndpointTest {
  @Mock private ContainerConfig container1;
  @Mock private ContainerConfig container2;
  @Mock private EnvironmentConfigProps environment2;
  @Mock private ProcessService processService;
  @Mock private ContainerService containerService;
  @Mock private RServerConnection connection;

  @Test
  void testDoWithConnection() {
    var endpoint =
        new RProcessEndpoint(processService, containerService) {
          EnvironmentConfigProps selectedEnvironment = null;

          @Override
          RServerConnection connect(EnvironmentConfigProps environment) {
            selectedEnvironment = environment;
            return connection;
          }
        };

    when(containerService.getAll()).thenReturn(List.of(container1, container2));
    when(container1.getName()).thenReturn("kick");
    when(container2.getName()).thenReturn("windsock");
    when(container2.toEnvironmentConfigProps()).thenReturn(environment2);
    when(environment2.getName()).thenReturn("windsock");

    assertSame(connection, endpoint.doWithConnection("windsock", connection -> connection));
    assertEquals(container2.getName(), endpoint.selectedEnvironment.getName());
  }
}
