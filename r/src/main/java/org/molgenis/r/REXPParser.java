package org.molgenis.r;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.RList;
import org.springframework.stereotype.Component;

/** Parses REXP objects returned from Rserve */
@Component
public class REXPParser {

  /**
   * Parses 2D R string matrix to a list of String maps
   *
   * @param matrix REXP containing the matrix
   * @return List of {@link Map}s mapping column name to value for each row
   * @throws REXPMismatchException if the REXP is not of the expected shape
   */
  public List<Map<String, String>> toStringMap(REXPString matrix) throws REXPMismatchException {
    RList dimnames = matrix.getAttribute("dimnames").asList();
    List<String> colNames = asList(((REXPString) dimnames.get(1)).asStrings());
    List<String> values = asList(matrix.asStrings());
    int numRows = values.size() / colNames.size();
    if (numRows == 0) {
      return emptyList();
    }
    List<List<String>> columnValues = Lists.partition(values, numRows);
    List<Map<String, String>> result = new ArrayList<>();
    for (int rowNum = 0; rowNum < numRows; rowNum++) {
      Map<String, String> row = new LinkedHashMap<>();
      for (int colNum = 0; colNum < colNames.size(); colNum++) {
        String colName = colNames.get(colNum);
        String value = columnValues.get(colNum).get(rowNum);
        if (value != null) {
          row.put(colName, value);
        }
      }
      result.add(row);
    }
    return result;
  }
}
