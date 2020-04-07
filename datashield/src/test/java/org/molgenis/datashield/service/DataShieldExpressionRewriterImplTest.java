package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.core.impl.PackagedFunctionDSMethod;

@ExtendWith(MockitoExtension.class)
class DataShieldExpressionRewriterImplTest {

  private DataShieldExpressionRewriterImpl expressionRewriter;

  @Mock private DataShieldEnvironmentHolder environmentHolder;
  @Mock private DSEnvironment mockEnvironment;

  @BeforeEach
  public void beforeEach() {
    when(environmentHolder.getEnvironment(any(DSMethodType.class))).thenReturn(mockEnvironment);
    expressionRewriter = new DataShieldExpressionRewriterImpl(environmentHolder);
  }

  @Test
  void testRewriteAssignDsBase() {
    DSMethod meanDS = new PackagedFunctionDSMethod("meanDS", "dsBase::meanDS");
    when(mockEnvironment.getMethod("meanDS")).thenReturn(meanDS);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.ASSIGN);
    assertEquals("dsBase::meanDS(D$age)", expressionRewriter.rewriteAssign("meanDS(D$age)"));
  }

  @Test
  void testRewriteAssignNonDsBase() {
    DSMethod dim = new PackagedFunctionDSMethod("dim", "base::dim");
    when(mockEnvironment.getMethod("dim")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.ASSIGN);
    assertEquals("base::dim(x, y)", expressionRewriter.rewriteAssign("dim(x, y)"));
  }

  @Test
  void testRewriteAssignUnknown() {
    when(mockEnvironment.getMethod("banana")).thenThrow(NoSuchDSMethodException.class);
    assertThrows(
        NoSuchDSMethodException.class, () -> expressionRewriter.rewriteAggregate("banana(x,y)"));
  }

  @Test
  void testRewriteAggregateDsBase() {
    DSMethod dim = new PackagedFunctionDSMethod("scatterPlotDs", "dsBase::scatterPlotDs");
    when(mockEnvironment.getMethod("scatterPlotDs")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.AGGREGATE);
    assertEquals(
        "dsBase::scatterPlotDs(D$age, D$potatoes_a_day)",
        expressionRewriter.rewriteAggregate("scatterPlotDs(D$age, D$potatoes_a_day)"));
  }

  @Test
  void testRewriteAggregateNonDsBase() {
    DSMethod dim = new PackagedFunctionDSMethod("is.character", "base::is.character");
    when(mockEnvironment.getMethod("is.character")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.AGGREGATE);
    assertEquals("base::is.character(3)", expressionRewriter.rewriteAggregate("is.character(3)"));
  }

  @Test
  void testRewriteFaultyExpression() {
    assertThrows(
        DataShieldExpressionException.class, () -> expressionRewriter.rewriteAggregate("meanDS(="));
  }
}
