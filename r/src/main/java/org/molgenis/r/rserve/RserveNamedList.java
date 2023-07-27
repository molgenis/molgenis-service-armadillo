package org.molgenis.r.rserve;

import static com.google.common.collect.Lists.newArrayList;
import static org.rosuda.REngine.REXPLogical.TRUE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.*;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerResult;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;

@VisibleForTesting
public class RserveNamedList implements RNamedList<RServerResult> {

  private final Map<String, RServerResult> namedList = Maps.newLinkedHashMap();

  private List<String> names = Lists.newArrayList();

  public RserveNamedList(REXP rexp) throws REXPMismatchException {
    if (rexp != null && rexp.isList()) {
      RList list = rexp.asList();
      initialize(list);
    }
  }

  public RserveNamedList(RList list) {
    initialize(list);
  }

  @Override
  public List<String> getNames() {
    return names;
  }

  @Override
  public int size() {
    return namedList.size();
  }

  @Override
  public boolean isEmpty() {
    return namedList.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    return namedList.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    return namedList.containsValue(o);
  }

  @Override
  public RServerResult get(Object o) {
    return namedList.get(o);
  }

  @Override
  public Set<String> keySet() {
    return namedList.keySet();
  }

  @Override
  public Collection<RServerResult> values() {
    return namedList.values();
  }

  @Override
  public Set<Map.Entry<String, RServerResult>> entrySet() {
    return namedList.entrySet();
  }

  @Override
  public List<Map<String, Object>> asRows() {
    List<Map<String, Object>> rows = newArrayList();
    if (names.isEmpty()) return rows;

    var numRows = namedList.get(names.get(0)).length();
    for (int rowNum = 0; rowNum < numRows; rowNum++) {
      Map<String, Object> converted = new LinkedHashMap<>();
      rows.add(converted);
      for (String name : names) {
        RServerResult values = namedList.get(name);
        getValueAtIndex(values, rowNum).ifPresent(value -> converted.put(name, value));
      }
    }
    return rows;
  }

  private Optional<Object> getValueAtIndex(RServerResult values, int rowNum) {
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

  private void initialize(RList list) {
    if (list.isNamed()) {
      this.names =
          ((List<Object>) list.names).stream().map(n -> n == null ? null : n.toString()).toList();
      for (int i = 0; i < list.names.size(); i++) {
        String key = list.names.get(i).toString();
        REXP value = list.at(key);
        namedList.put(key, new RserveResult(value));
      }
    }
  }
}
