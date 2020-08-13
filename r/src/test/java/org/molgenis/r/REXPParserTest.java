package org.molgenis.r;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;

class REXPParserTest {

  private REXPParser rexpParser = new REXPParser();

  private REXPString rownames = new REXPString(new String[] {"base", "desc"});
  REXPString colnames = new REXPString(new String[] {"Package", "Version"});
  RList dimnames = new RList(new REXP[] {rownames, colnames});
  REXPGenericVector dimnamesVector = new REXPGenericVector(dimnames);
  REXPList attributes = new REXPList(dimnamesVector, "dimnames");

  @Test
  void toStringMapEmptyList() throws REXPMismatchException {
    REXPString rexpString = new REXPString(new String[] {}, attributes);
    assertEquals(emptyList(), rexpParser.toStringMap(rexpString));
  }

  @Test
  void toStringMapPackageMatrix() throws REXPMismatchException {
    REXPString rexpString =
        new REXPString(new String[] {"base", "desc", "1.0.1", "1.2.3"}, attributes);
    assertEquals(
        List.of(
            Map.of("Package", "base", "Version", "1.0.1"),
            Map.of("Package", "desc", "Version", "1.2.3")),
        rexpParser.toStringMap(rexpString));
  }

  @Test
  void parseTibbleTransposesTheDataStructure() throws REXPMismatchException {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {"label1", "label2"})),
            new String[] {"id", "label"});
    var parsed = rexpParser.parseTibble(rList);

    assertEquals(
        List.of(Map.of("id", "id1", "label", "label1"), Map.of("id", "id2", "label", "label2")),
        parsed);
  }

  @Test
  void parseTibbleSkipsNAValues() throws REXPMismatchException {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPLogical(new byte[] {REXPLogical.NA, REXPLogical.FALSE})),
            new String[] {"id", "valid"});
    var parsed = rexpParser.parseTibble(rList);

    assertEquals(List.of(Map.of("id", "id1"), Map.of("id", "id2", "valid", false)), parsed);
  }

  @Test
  void parseTibbleSkipsNullValues() throws REXPMismatchException {
    RList rList =
        new RList(
            List.of(
                new REXPString(new String[] {"id1", "id2"}),
                new REXPString(new String[] {null, "label2"})),
            new String[] {"id", "label"});
    var parsed = rexpParser.parseTibble(rList);

    assertEquals(List.of(Map.of("id", "id1"), Map.of("id", "id2", "label", "label2")), parsed);
  }

  @ParameterizedTest
  @MethodSource("logicalsProvider")
  void getValueAtIndexParsesLogicals(byte logical, Optional<Boolean> expected)
      throws REXPMismatchException {
    var parsed = rexpParser.getValueAtIndex(new REXPLogical(logical), 0);
    assertEquals(expected, parsed);
  }

  static Stream<Arguments> logicalsProvider() {
    return Stream.of(
        Arguments.of(REXPLogical.TRUE, Optional.of(true)),
        Arguments.of(REXPLogical.FALSE, Optional.of(false)),
        Arguments.of(REXPLogical.NA, Optional.<Boolean>empty()));
  }

  @ParameterizedTest
  @MethodSource("doublesProvider")
  void getValueAtIndexParsesDoubles(Double value, Optional<Double> expected)
      throws REXPMismatchException {
    var parsed = rexpParser.getValueAtIndex(new REXPDouble(value), 0);
    assertEquals(expected, parsed);
  }

  static Stream<Arguments> doublesProvider() {
    return Stream.of(
        Arguments.of(0.0, Optional.of(0.0)), Arguments.of(REXPDouble.NA, Optional.empty()));
  }

  @ParameterizedTest
  @MethodSource("intsProvider")
  void getValueAtIndexParsesIntegers(int value, Optional<Integer> expected)
      throws REXPMismatchException {
    var parsed = rexpParser.getValueAtIndex(new REXPInteger(value), 0);
    assertEquals(expected, parsed);
  }

  static Stream<Arguments> intsProvider() {
    return Stream.of(
        Arguments.of(0, Optional.of(0)), Arguments.of(REXPInteger.NA, Optional.empty()));
  }

  @Test
  void getValueAtIndexSkipsTheUnknown() throws REXPMismatchException {
    var vector = new REXPGenericVector(dimnames);
    var parsed = rexpParser.getValueAtIndex(vector, 0);
    assertEquals(Optional.empty(), parsed);
  }
}
