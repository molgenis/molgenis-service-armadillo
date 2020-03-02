package org.molgenis.datashield.r;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.service.model.PackageTest.BASE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.service.PackageService;
import org.molgenis.datashield.service.model.Package;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class DataShieldOptionsImplTest {

  private RConfigProperties configProperties = new RConfigProperties();
  @Mock private PackageService packageService;
  @Mock private RConnection rConnection;

  DataShieldOptionsImpl options;

  @BeforeEach
  public void beforeEach() {
    options = spy(new DataShieldOptionsImpl(configProperties, packageService));
  }

  @Test
  void init() throws REXPMismatchException, RserveException {
    ImmutableMap<String, String> configOptions =
        ImmutableMap.of("a", "overrideA", "c", "overrideC");
    configProperties.setOptions(configOptions);
    ImmutableMap<String, String> packageOptions = ImmutableMap.of("a", "defaultA", "b", "defaultB");
    doReturn(rConnection).when(options).createConnection();

    Package datashieldPackage =
        Package.builder()
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
  }
}
