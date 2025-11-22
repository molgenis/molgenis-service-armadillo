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
  @Mock private ContainerConfig profile1;
  @Mock private ContainerConfig profile2;
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

    when(containerService.getAll()).thenReturn(List.of(profile1, profile2));
    when(profile1.getName()).thenReturn("kick");
    when(profile2.getName()).thenReturn("windsock");
    when(profile2.toEnvironmentConfigProps()).thenReturn(environment2);
    when(environment2.getName()).thenReturn("windsock");

    assertSame(connection, endpoint.doWithConnection("windsock", connection -> connection));
    assertEquals(profile2.getName(), endpoint.selectedEnvironment.getName());
  }
}
