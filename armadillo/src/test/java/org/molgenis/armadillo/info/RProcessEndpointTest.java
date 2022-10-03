package org.molgenis.armadillo.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class RProcessEndpointTest {
  @Mock private ProfileConfig environment1;
  @Mock private ProfileConfig environment2;
  @Mock private ProcessService processService;
  @Mock private ProfileService profileService;
  @Mock private RConnection connection;

  @Test
  void testDoWithConnection() {
    var endpoint =
        new RProcessEndpoint(processService, profileService) {
          EnvironmentConfigProps selectedEnvironment = null;

          @Override
          RConnection connect(EnvironmentConfigProps environment) {
            selectedEnvironment = environment;
            return connection;
          }
        };

    when(profileService.getAll()).thenReturn(List.of(environment1, environment2));
    when(environment1.getName()).thenReturn("kick");
    when(environment2.getName()).thenReturn("windsock");

    assertSame(connection, endpoint.doWithConnection("windsock", connection -> connection));
    assertEquals(environment2.getName(), endpoint.selectedEnvironment.getName());
  }
}
