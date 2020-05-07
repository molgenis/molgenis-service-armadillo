package org.molgenis.armadillo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.DataControllerTest.BASE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class ArmadilloOptionsImplTest {

  private ArmadilloProperties armadilloProperties = new ArmadilloProperties();
  @Mock private PackageService packageService;
  @Mock private RConnectionFactory rConnectionFactory;
  @Mock private RConnection rConnection;

  ArmadilloOptionsImpl options;

  @BeforeEach
  public void beforeEach() {
    options = new ArmadilloOptionsImpl(armadilloProperties, packageService, rConnectionFactory);
  }

  @Test
  void init() throws REXPMismatchException, RserveException {
    ImmutableMap<String, String> configOptions =
        ImmutableMap.of("a", "overrideA", "c", "overrideC");
    armadilloProperties.setOptions(configOptions);
    ImmutableMap<String, String> packageOptions = ImmutableMap.of("a", "defaultA", "b", "defaultB");
    doReturn(rConnection).when(rConnectionFactory).retryCreateConnection();

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
