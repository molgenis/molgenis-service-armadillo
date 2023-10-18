package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.r.rserve.RserveNamedList;
import org.rosuda.REngine.*;

class RserveNamedListTest {

  private REXPString rownames = new REXPString(new String[] {"base", "desc"});
  REXPString colnames = new REXPString(new String[] {"Package", "Version"});
  RList dimnames = new RList(new REXP[] {rownames, colnames});

  @Test
  void parseTibbleTransposesTheDataStructure() throws RServerException {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {"label1", "label2"})),
            new String[] {"id", "label"});
    var parsed = new RserveNamedList(rList).asRows();

    assertEquals(
        List.of(Map.of("id", "id1", "label", "label1"), Map.of("id", "id2", "label", "label2")),
        parsed);
  }

  @Test
  void parseTibbleSkipsNAValues() {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPLogical(new byte[] {REXPLogical.NA, REXPLogical.FALSE})),
            new String[] {"id", "valid"});
    var parsed = new RserveNamedList(rList).asRows();

    assertEquals(List.of(Map.of("id", "id1"), Map.of("id", "id2", "valid", false)), parsed);
  }

  @Test
  void parseTibbleSkipsNullValues() {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {null, "label2"})),
            new String[] {"id", "label"});
    var parsed = new RserveNamedList(rList).asRows();

    assertEquals(List.of(Map.of("id", "id1"), Map.of("id", "id2", "label", "label2")), parsed);
  }

  @ParameterizedTest
  @MethodSource("logicalsProvider")
  void getValueAtIndexParsesLogicals(byte value, Optional<Boolean> expected) {
    RList rList = new RList(List.of(new REXPLogical(new byte[] {value})), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertEquals(expected.orElse(null), parsed.get(0).get("id"));
  }

  static Stream<Arguments> logicalsProvider() {
    return Stream.of(
        Arguments.of(REXPLogical.TRUE, Optional.of(true)),
        Arguments.of(REXPLogical.FALSE, Optional.of(false)),
        Arguments.of(REXPLogical.NA, Optional.<Boolean>empty()));
  }

  @ParameterizedTest
  @MethodSource("doublesProvider")
  void getValueAtIndexParsesDoubles(Double value, Optional<Double> expected) {
    RList rList = new RList(List.of(new REXPDouble(new double[] {value})), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertEquals(expected.orElse(null), parsed.get(0).get("id"));
  }

  static Stream<Arguments> doublesProvider() {
    return Stream.of(
        Arguments.of(0.0, Optional.of(0.0)), Arguments.of(REXPDouble.NA, Optional.empty()));
  }

  @ParameterizedTest
  @MethodSource("intsProvider")
  void getValueAtIndexParsesIntegers(int value, Optional<Integer> expected) {
    RList rList = new RList(List.of(new REXPInteger(new int[] {value})), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertEquals(expected.orElse(null), parsed.get(0).get("id"));
  }

  static Stream<Arguments> intsProvider() {
    return Stream.of(
        Arguments.of(0, Optional.of(0)), Arguments.of(REXPInteger.NA, Optional.empty()));
  }

  @Test
  void getValueAtIndexSkipsTheUnknown() throws REXPMismatchException {
    var vector = new REXPGenericVector(dimnames);
    RList rList = new RList(List.of(vector), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertNull(parsed.get(0).get("id"));
  }
}
