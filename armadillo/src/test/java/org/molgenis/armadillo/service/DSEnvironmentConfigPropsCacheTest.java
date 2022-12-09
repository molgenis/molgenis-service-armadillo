package org.molgenis.armadillo.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.DuplicateRMethodException;
import org.molgenis.armadillo.exceptions.IllegalRMethodStringException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.DefaultDSMethod;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
class DSEnvironmentConfigPropsCacheTest {

  @Mock RConnectionFactory rConnectionFactory;
  @Mock PackageService packageService;
  @Mock ProfileConfig profileConfig;
  private DSEnvironmentCache dsEnvironmentCache;

  @BeforeEach
  void beforeEach() {
    dsEnvironmentCache = new DSEnvironmentCache(packageService, rConnectionFactory, profileConfig);
  }

  @Test
  void testGetAggregateEnvironment() {
    when(profileConfig.getPackageWhitelist()).thenReturn(Set.of("dsBase"));
    populateEnvironment(
        ImmutableSet.of("scatterPlotDs", "is.character=base::is.character"), ImmutableSet.of());

    DSEnvironment environment = dsEnvironmentCache.getEnvironment(DSMethodType.AGGREGATE);

    assertEquals(
        asList("scatterPlotDs", "is.character"),
        environment.getMethods().stream().map(DSMethod::getName).collect(toList()));
  }

  @Test
  void testGetAssignEnvironment() {
    when(profileConfig.getPackageWhitelist()).thenReturn(Set.of("dsBase"));
    populateEnvironment(ImmutableSet.of(), ImmutableSet.of("meanDS", "dim=base::dim"));

    DSEnvironment environment = dsEnvironmentCache.getEnvironment(DSMethodType.ASSIGN);

    var expected =
        asList(
            new DefaultDSMethod("meanDS", "dsBase::meanDS", "dsBase", "1.2.3"),
            new DefaultDSMethod("dim", "base::dim", "base", null));
    var actual = environment.getMethods();

    // TODO: workaround until equals method implemented in DSMethod:
    Gson gson = new Gson();
    assertEquals(
        expected.stream().map(gson::toJson).collect(toList()),
        actual.stream().map(gson::toJson).collect(toList()));
  }

  @Test
  void testPopulateIllegalMethodName() {
    when(profileConfig.getPackageWhitelist()).thenReturn(Set.of("dsBase"));
    final var aggregateMethods = ImmutableSet.of("method=base::method=base::method");
    final ImmutableSet<String> assignMethods = ImmutableSet.of();
    assertThrows(
        IllegalRMethodStringException.class,
        () -> populateEnvironment(aggregateMethods, assignMethods));
  }

  @Test
  void testPopulateDuplicateMethodName() {
    when(profileConfig.getPackageWhitelist()).thenReturn(Set.of("dsBase"));
    final var aggregateMethods = ImmutableSet.of("dim=base::dim", "dim=other::dim");
    final ImmutableSet<String> assignMethods = ImmutableSet.of();
    assertThrows(
        DuplicateRMethodException.class,
        () -> populateEnvironment(aggregateMethods, assignMethods));
  }

  @Test
  void testPopulateMethodFromNonWhitelistedPackage() {
    when(profileConfig.getPackageWhitelist()).thenReturn(Set.of("otherPackage"));
    final var aggregateMethods = ImmutableSet.of("dim=base::dim");
    final ImmutableSet<String> assignMethods = ImmutableSet.of();
    populateEnvironment(aggregateMethods, assignMethods);

    DSEnvironment environment = dsEnvironmentCache.getEnvironment(DSMethodType.ASSIGN);
    assertEquals(0, environment.getMethods().size());
  }

  private void populateEnvironment(
      ImmutableSet<String> aggregateMethods, ImmutableSet<String> assignMethods) {
    RConnection rConnection = mock(RConnection.class);
    when(rConnectionFactory.tryCreateConnection()).thenReturn(rConnection);

    RPackage pack =
        RPackage.builder()
            .setAggregateMethods(aggregateMethods)
            .setAssignMethods(assignMethods)
            .setLibPath("test")
            .setVersion("test")
            .setName("dsBase")
            .setBuilt("test")
            .setVersion("1.2.3")
            .build();

    when(packageService.getInstalledPackages(rConnection)).thenReturn(singletonList(pack));

    dsEnvironmentCache.populateEnvironments();
    verify(rConnection).close();
  }
}
