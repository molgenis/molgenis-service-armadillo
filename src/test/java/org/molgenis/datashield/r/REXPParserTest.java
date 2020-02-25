package org.molgenis.datashield.r;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REXPList;
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
}
