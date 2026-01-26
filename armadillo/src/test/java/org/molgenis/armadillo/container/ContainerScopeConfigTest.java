package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.molgenis.armadillo.exceptions.UnsupportedContainerTypeException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.r.RConnectionFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

@ExtendWith(MockitoExtension.class)
class ContainerScopeConfigTest {

  @Mock private ContainerService containerService;
  @Mock private DatashieldRConnectionFactoryProvider datashieldProvider;
  @Mock private RConnectionFactory rConnectionFactory;
  @Mock private ContainerScope containerScope;
  @Mock private ConfigurableListableBeanFactory beanFactory;

  private final ContainerScopeConfig config = new ContainerScopeConfig();

  @AfterEach
  void resetActiveContainerName() {
    ActiveContainerNameAccessor.resetActiveContainerName();
  }

  @Test
  void containerConfig_usesActiveContainerName() {
    ActiveContainerNameAccessor.setActiveContainerName("donkey");
    var expected = mock(ContainerConfig.class);
    when(containerService.getByName("donkey")).thenReturn(expected);

    ContainerConfig result = config.containerConfig(containerService);

    assertSame(expected, result);
    verify(containerService).getByName("donkey");
  }

  @Test
  void containerConfig_throwsWhenContainerMissing() {
    ActiveContainerNameAccessor.setActiveContainerName("missing");
    when(containerService.getByName("missing")).thenThrow(new UnknownContainerException("nope"));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> config.containerConfig(containerService));

    assertTrue(ex.getMessage().contains("missing"));
  }

  @Test
  void datashieldContainerConfig_acceptsDatashieldConfig() {
    var datashieldConfig = createDatashieldConfig();

    DatashieldContainerConfig result = config.datashieldContainerConfig(datashieldConfig);

    assertSame(datashieldConfig, result);
  }

  @Test
  void datashieldContainerConfig_rejectsNonDatashield() {
    ContainerConfig nonDatashield = mock(ContainerConfig.class);

    assertThrows(
        UnsupportedContainerTypeException.class,
        () -> config.datashieldContainerConfig(nonDatashield));
  }

  @Test
  void rConnectionFactory_delegatesToProvider() {
    var datashieldConfig = createDatashieldConfig();
    when(datashieldProvider.create(datashieldConfig)).thenReturn(rConnectionFactory);

    RConnectionFactory result = config.rConnectionFactory(datashieldConfig, datashieldProvider);

    assertSame(rConnectionFactory, result);
    verify(datashieldProvider).create(datashieldConfig);
  }

  @Test
  void beanFactoryPostProcessor_registersContainerScope() {
    BeanFactoryPostProcessor processor =
        ContainerScopeConfig.beanFactoryPostProcessor(containerScope);

    processor.postProcessBeanFactory(beanFactory);

    verify(beanFactory).registerScope("container", containerScope);
  }

  private static DatashieldContainerConfig createDatashieldConfig() {
    return DatashieldContainerConfig.builder()
        .name("default")
        .packageWhitelist(Set.of())
        .functionBlacklist(Set.of())
        .build();
  }
}
