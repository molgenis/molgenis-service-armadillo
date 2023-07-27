package org.molgenis.r.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXPMismatchException;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {
  static RPackage BASE =
      RPackage.builder()
          .setName("base")
          .setVersion("3.6.1")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  static RPackage DESC =
      RPackage.builder()
          .setName("desc")
          .setVersion("1.2.0")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  @Mock private RServerResult rexp;
  @Mock private RNamedList<RServerResult> rlist;
  @Mock private RServerConnection rConnection;

  private PackageService packageService;

  @BeforeEach
  void before() {
    packageService = new PackageServiceImpl();
  }

  @Test
  void testGetInstalledPackages() throws RServerException {
    when(rConnection.eval(anyString())).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(rlist);
    when(rlist.asRows())
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
  void testParseSimpleOptions() throws REXPMismatchException, RServerException {
    when(rConnection.eval(anyString())).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(rlist);
    when(rlist.asRows())
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
  void testParseSkipsEmptyStrings() throws RServerException {
    when(rConnection.eval(anyString())).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(rlist);
    when(rlist.asRows())
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
                    "",
                    "AssignMethods",
                    "",
                    "AggregateMethods",
                    "")));
    assertEquals(
        RPackage.builder().setName("p").setVersion("v").setBuilt("b").setLibPath("l").build(),
        packageService.getInstalledPackages(rConnection).get(0));
  }

  @Test
  void testParseAssignMethods() throws RServerException {
    when(rConnection.eval(anyString())).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(rlist);
    when(rlist.asRows())
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
  void testParseAggregateMethods() throws RServerException {
    when(rConnection.eval(anyString())).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(rlist);
    when(rlist.asRows())
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

  @Test
  void testLoadPackages() throws RServerException {
    Set<String> packages = ImmutableSet.of("dsBase", "dsExposome");
    String command =
        "base::lapply(c("
            + String.format("\"%s\"", String.join("\",\"", packages))
            + "), library, character.only = TRUE)";
    packageService.loadPackages(rConnection, packages);
    verify(rConnection, atLeastOnce()).eval(command);
  }
}
