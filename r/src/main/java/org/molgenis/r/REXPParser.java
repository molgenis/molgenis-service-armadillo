package org.molgenis.r;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.rosuda.REngine.REXPLogical.TRUE;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;
import org.springframework.stereotype.Component;

/** Parses REXP objects returned from Rserve */
@Component
public class REXPParser {

  public Optional<Instant> parseDate(Double date) {
    return Optional.ofNullable(date).map(it -> Math.round(it * 1000)).map(Instant::ofEpochMilli);
  }

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

  public List<Map<String, Object>> parseTibble(RList list) throws REXPMismatchException {
    List<Map<String, Object>> rows = newArrayList();
    var numRows = ((REXPVector) list.get(list.names.get(0))).length();
    for (int rowNum = 0; rowNum < numRows; rowNum++) {
      Map<String, Object> converted = new LinkedHashMap<>();
      rows.add(converted);
      for (Object key : list.names) {
        String name = (String) key;
        REXPVector values = (REXPVector) list.get(name);
        getValueAtIndex(values, rowNum).ifPresent(value -> converted.put(name, value));
      }
    }
    return rows;
  }

  Optional<Object> getValueAtIndex(REXPVector values, int rowNum) throws REXPMismatchException {
    if (values.isNA()[rowNum]) {
      return Optional.empty();
    }
    if (values.isInteger()) {
      return Optional.of(values.asIntegers()[rowNum]);
    } else if (values.isLogical()) {
      return Optional.of(values.asIntegers()[rowNum]).map(it -> Integer.valueOf(TRUE).equals(it));
    } else if (values.isNumeric()) {
      return Optional.of(values.asDoubles()[rowNum]);
    } else if (values.isString()) {
      return Optional.ofNullable(values.asStrings()[rowNum]);
    } else {
      return Optional.empty();
    }
  }
}
