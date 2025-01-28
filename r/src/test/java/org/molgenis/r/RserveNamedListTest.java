package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.*;

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

  static Stream<Arguments> intsProvider() {
    return Stream.of(
        Arguments.of(0, Optional.of(0)), Arguments.of(REXPInteger.NA, Optional.empty()));
  }

  static Stream<Arguments> logicalsProvider() {
    return Stream.of(
        Arguments.of(REXPLogical.TRUE, Optional.of(true)),
        Arguments.of(REXPLogical.FALSE, Optional.of(false)),
        Arguments.of(REXPLogical.NA, Optional.<Boolean>empty()));
  }

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
  void testParseTibbleSkipsNAValues() {
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
  void testParseTibbleSkipsNullValues() {
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
  void testGetValueAtIndexParsesLogicals(byte value, Optional<Boolean> expected) {
    RList rList = new RList(List.of(new REXPLogical(new byte[] {value})), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertEquals(expected.orElse(null), parsed.get(0).get("id"));
  }

  @ParameterizedTest
  @MethodSource("doublesProvider")
  void testGetValueAtIndexParsesDoubles(Double value, Optional<Double> expected) {
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
  void testGetValueAtIndexParsesIntegers(int value, Optional<Integer> expected) {
    RList rList = new RList(List.of(new REXPInteger(new int[] {value})), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertEquals(expected.orElse(null), parsed.get(0).get("id"));
  }

  @Test
  void testGetValueAtIndexSkipsTheUnknown() throws REXPMismatchException {
    var vector = new REXPGenericVector(dimnames);
    RList rList = new RList(List.of(vector), new String[] {"id"});
    var parsed = new RserveNamedList(rList).asRows();
    assertNull(parsed.get(0).get("id"));
  }

  @Test
  void testRserveNamedList() {
    assertDoesNotThrow(() -> new RserveNamedList(new REXP()));
  }

  @Test
  void testIsEmpty() {
    RList rList = new RList();
    assertTrue(new RserveNamedList(rList).isEmpty());
  }

  @Test
  void testSize() {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {null, "label2"})),
            new String[] {"id", "label"});
    RserveNamedList list = new RserveNamedList(rList);
    assertEquals(2, list.size());
  }

  @Test
  void testContainsKey() {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {null, "label2"})),
            new String[] {"id", "label"});
    RserveNamedList list = new RserveNamedList(rList);
    assertTrue(list.containsKey("id"));
  }

  @Test
  void asRowsEmptyNames() throws RServerException {
    RList rList = new RList();
    var parsed = new RserveNamedList(rList);
    assertTrue(parsed.isEmpty());
  }
}
