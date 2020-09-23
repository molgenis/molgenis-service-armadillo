package org.molgenis.r;

import static com.google.common.collect.Lists.newArrayList;
import static org.rosuda.REngine.REXPLogical.TRUE;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.RList;
import org.springframework.stereotype.Component;

/** Parses REXP objects returned from Rserve */
@Component
public class REXPParser {

  public Optional<Instant> parseDate(Double date) {
    return Optional.ofNullable(date).map(it -> Math.round(it * 1000)).map(Instant::ofEpochMilli);
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
