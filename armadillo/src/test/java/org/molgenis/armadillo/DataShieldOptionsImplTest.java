package org.molgenis.armadillo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class DataShieldOptionsImplTest {

  private static RPackage BASE =
      RPackage.builder()
          .setName("base")
          .setVersion("3.6.1")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  private final ProfileConfigProps profileConfigProps = new ProfileConfigProps();
  @Mock private PackageService packageService;
  @Mock private RConnectionFactory rConnectionFactory;
  @Mock private RConnection rConnection;

  DataShieldOptionsImpl options;

  @BeforeEach
  void beforeEach() {
    options = new DataShieldOptionsImpl(profileConfigProps, packageService, rConnectionFactory);
  }

  @Test
  void init() {
    ImmutableMap<String, String> configOptions =
        ImmutableMap.of("a", "overrideA", "c", "overrideC");
    profileConfigProps.setOptions(configOptions);
    ImmutableMap<String, String> packageOptions = ImmutableMap.of("a", "defaultA", "b", "defaultB");
    doReturn(rConnection).when(rConnectionFactory).tryCreateConnection();

    RPackage datashieldPackage =
        RPackage.builder()
            .setName("dsBase")
            .setVersion("1.2.3")
            .setBuilt("3.2.1")
            .setLibPath("/var/lib/R")
            .setOptions(packageOptions)
            .build();
    when(packageService.getInstalledPackages(rConnection))
        .thenReturn(ImmutableList.of(datashieldPackage, BASE));
    options.init();
    assertEquals(
        options.getValue(), ImmutableMap.of("a", "overrideA", "b", "defaultB", "c", "overrideC"));
    verify(rConnection).close();
  }
}
