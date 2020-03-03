package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.molgenis.r.model.PackageTest.BASE;
import static org.molgenis.r.model.PackageTest.DESC;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.PackageServiceImpl;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {
  @Mock private REXPParser rexpParser;
  @Mock private REXPString rexp;
  @Mock private RConnection rConnection;

  private PackageService packageService;

  @BeforeEach
  void before() {
    packageService = new PackageServiceImpl(rexpParser);
  }

  @Test
  public void testGetInstalledPackages() throws REXPMismatchException, RserveException {
    when(rConnection.eval(
            "installed.packages(fields=c(\"AggregateMethods\",\"AssignMethods\",\"Options\"))"))
        .thenReturn(rexp);
    when(rexpParser.toStringMap(rexp))
        .thenReturn(
            List.of(
                Map.of(
                    "Package",
                    BASE.name(),
                    "Version",
                    BASE.version(),
                    "Built",
                    BASE.built(),
                    "LibPath",
                    BASE.libPath()),
                Map.of(
                    "Package",
                    DESC.name(),
                    "Version",
                    DESC.version(),
                    "Built",
                    DESC.built(),
                    "LibPath",
                    DESC.libPath())));
    assertEquals(List.of(BASE, DESC), packageService.getInstalledPackages(rConnection));
  }

  @Test
  public void testParseSimpleOptions() throws REXPMismatchException, RserveException {
    when(rConnection.eval(
            "installed.packages(fields=c(\"AggregateMethods\",\"AssignMethods\",\"Options\"))"))
        .thenReturn(rexp);
    when(rexpParser.toStringMap(rexp))
        .thenReturn(
            List.of(
                Map.of(
                    "Package",
                    "p",
                    "Version",
                    "v",
                    "Built",
                    "b",
                    "LibPath",
                    "l",
                    "Options",
                    "default.nfilter.string=80,default.nfilter.kNN=3")));
    assertEquals(
        Map.of("default.nfilter.string", "80", "default.nfilter.kNN", "3"),
        packageService.getInstalledPackages(rConnection).get(0).options());
  }

  @Test
  public void testParseAssignMethods() throws REXPMismatchException, RserveException {
    when(rConnection.eval(
            "installed.packages(fields=c(\"AggregateMethods\",\"AssignMethods\",\"Options\"))"))
        .thenReturn(rexp);
    when(rexpParser.toStringMap(rexp))
        .thenReturn(
            List.of(
                Map.of(
                    "Package",
                    "p",
                    "Version",
                    "v",
                    "Built",
                    "b",
                    "LibPath",
                    "l",
                    "AssignMethods",
                    "subsetByClassDS, cbind=base::cbind")));
    assertEquals(
        Set.of("subsetByClassDS", "cbind=base::cbind"),
        packageService.getInstalledPackages(rConnection).get(0).assignMethods());
  }

  @Test
  public void testParseAggregateMethods() throws REXPMismatchException, RserveException {
    when(rConnection.eval(
            "installed.packages(fields=c(\"AggregateMethods\",\"AssignMethods\",\"Options\"))"))
        .thenReturn(rexp);
    when(rexpParser.toStringMap(rexp))
        .thenReturn(
            List.of(
                Map.of(
                    "Package",
                    "p",
                    "Version",
                    "v",
                    "Built",
                    "b",
                    "LibPath",
                    "l",
                    "AggregateMethods",
                    "is.numeric=base::is.numeric,meanSdGpDS")));
    assertEquals(
        Set.of("is.numeric=base::is.numeric", "meanSdGpDS"),
        packageService.getInstalledPackages(rConnection).get(0).aggregateMethods());
  }
}
