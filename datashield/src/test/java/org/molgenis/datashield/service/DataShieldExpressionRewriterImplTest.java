package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.Package;
import org.molgenis.r.service.PackageService;
import org.obiba.datashield.core.NoSuchDSMethodException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@ExtendWith(MockitoExtension.class)
class DataShieldExpressionRewriterImplTest {

  private DataShieldExpressionRewriterImpl expressionRewriter;

  @Mock private PackageService packageService;
  @Mock private RConnectionFactory rConnectionFactory;

  @BeforeEach
  public void beforeEach() throws REXPMismatchException, RserveException {
    expressionRewriter = new DataShieldExpressionRewriterImpl(packageService, rConnectionFactory);
    populateEnvironments();
  }

  @Test
  void testRewriteAssignDsBase() {
    assertEquals("dsBase::meanDS(D$age)", expressionRewriter.rewriteAssign("meanDS(D$age)"));
  }

  @Test
  void testRewriteAssignNonDsBase() {
    assertEquals("base::dim(x, y)", expressionRewriter.rewriteAssign("dim(x, y)"));
  }

  @Test
  void testRewriteAssignUnknown() {
    assertThrows(
        NoSuchDSMethodException.class, () -> expressionRewriter.rewriteAggregate("banana(x,y)"));
  }

  @Test
  void testRewriteAggregateDsBase() {
    assertEquals(
        "dsBase::scatterPlotDs(D$age, D$potatoes_a_day)",
        expressionRewriter.rewriteAggregate("scatterPlotDs(D$age, D$potatoes_a_day)"));
  }

  @Test
  void testRewriteAggregateNonDsBase() {
    assertEquals("base::is.character(3)", expressionRewriter.rewriteAggregate("is.character(3)"));
  }

  @Test
  void testRewriteFaultyExpression() {
    assertThrows(
        DataShieldExpressionException.class, () -> expressionRewriter.rewriteAggregate("meanDS(="));
  }

  private void populateEnvironments() throws REXPMismatchException, RserveException {
    RConnection rConnection = mock(RConnection.class);
    when(rConnectionFactory.retryCreateConnection()).thenReturn(rConnection);

    String assignDsBase = "meanDS";
    String assignNonDsBase = "dim=base::dim";
    String aggregateDsBase = "scatterPlotDs";
    String aggregateNonDsBase = "is.character=base::is.character";

    Package pack =
        Package.builder()
            .setAggregateMethods(ImmutableSet.of(aggregateDsBase, aggregateNonDsBase))
            .setAssignMethods(ImmutableSet.of(assignDsBase, assignNonDsBase))
            .setLibPath("test")
            .setVersion("test")
            .setName("test")
            .setBuilt("test")
            .build();

    when(packageService.getInstalledPackages(rConnection))
        .thenReturn(Collections.singletonList(pack));

    expressionRewriter.populateEnvironments();
  }
}
