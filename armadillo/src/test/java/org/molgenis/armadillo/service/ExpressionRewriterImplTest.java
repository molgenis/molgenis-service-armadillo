package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.ExpressionException;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.NoSuchDSMethodException;
import org.obiba.datashield.core.impl.DefaultDSMethod;

@ExtendWith(MockitoExtension.class)
class ExpressionRewriterImplTest {

  private ExpressionRewriterImpl expressionRewriter;

  @Mock private DataShieldProfileEnvironments environments;
  @Mock private DSEnvironment mockEnvironment;

  @BeforeEach
  void beforeEach() {
    when(environments.getEnvironment(any(DSMethodType.class))).thenReturn(mockEnvironment);
    expressionRewriter = new ExpressionRewriterImpl(environments);
  }

  @Test
  void testRewriteAssignDsBase() {
    DSMethod meanDS = new DefaultDSMethod("meanDS", "dsBase::meanDS", "dsBase", "1.2.3");
    when(mockEnvironment.getMethod("meanDS")).thenReturn(meanDS);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.ASSIGN);
    assertEquals("dsBase::meanDS(D$age)", expressionRewriter.rewriteAssign("meanDS(D$age)"));
  }

  @Test
  void testRewriteAssignNonDsBase() {
    DSMethod dim = new DefaultDSMethod("dim", "base::dim", "base", null);
    when(mockEnvironment.getMethod("dim")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.ASSIGN);
    assertEquals("base::dim(x, y)", expressionRewriter.rewriteAssign("dim(x, y)"));
  }

  @Test
  void testRewriteAssignUnknown() {
    when(mockEnvironment.getMethod("banana")).thenThrow(NoSuchDSMethodException.class);
    assertThrows(
        ExpressionException.class, () -> expressionRewriter.rewriteAggregate("banana(x,y)"));
  }

  @Test
  void testRewriteAggregateDsBase() {
    DSMethod dim = new DefaultDSMethod("scatterPlotDs", "dsBase::scatterPlotDs", "dsBase", "1.2.3");
    when(mockEnvironment.getMethod("scatterPlotDs")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.AGGREGATE);
    assertEquals(
        "dsBase::scatterPlotDs(D$age, D$potatoes_a_day)",
        expressionRewriter.rewriteAggregate("scatterPlotDs(D$age, D$potatoes_a_day)"));
  }

  @Test
  void testRewriteAggregateNonDsBase() {
    DSMethod dim = new DefaultDSMethod("is.character", "base::is.character", "base", null);
    when(mockEnvironment.getMethod("is.character")).thenReturn(dim);
    when(mockEnvironment.getMethodType()).thenReturn(DSMethodType.AGGREGATE);
    assertEquals("base::is.character(3)", expressionRewriter.rewriteAggregate("is.character(3)"));
  }

  @Test
  void testRewriteFaultyExpression() {
    assertThrows(ExpressionException.class, () -> expressionRewriter.rewriteAggregate("meanDS(="));
  }
}
