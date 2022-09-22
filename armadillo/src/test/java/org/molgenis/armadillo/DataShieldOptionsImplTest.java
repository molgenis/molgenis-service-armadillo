package org.molgenis.armadillo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ProfileConfig;
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

  @Mock private PackageService packageService;
  @Mock private RConnectionFactory rConnectionFactory;
  @Mock private RConnection rConnection;

  DataShieldOptionsImpl options;

  @Test
  void init() {
    ImmutableMap<String, String> configOptions =
        ImmutableMap.of("a", "overrideA", "c", "overrideC");
    ProfileConfig profileConfig =
        ProfileConfig.create("dummy", "dummy", 6311, Set.of(), configOptions, null);
    options = new DataShieldOptionsImpl(profileConfig, packageService, rConnectionFactory);
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
