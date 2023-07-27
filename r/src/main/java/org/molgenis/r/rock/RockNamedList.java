package org.molgenis.r.rock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerResult;

public class RockNamedList implements RNamedList<RServerResult> {

  private final Map<String, RServerResult> map = Maps.newLinkedHashMap();

  public RockNamedList(JSONObject objectResult) {
    Iterator<String> keys = objectResult.keys();
    while (keys.hasNext()) {
      String key = keys.next();
      Object value = objectResult.get(key);
      if (value instanceof JSONArray) {
        map.put(key, new RockResult((JSONArray) value));
      } else if (value instanceof JSONObject) {
        map.put(key, new RockResult((JSONObject) value));
      } else {
        map.put(key, new RockResult(value));
      }
    }
  }

  public RockNamedList(List<RServerResult> results) {
    // turn rows into columns
    Map<String, List<RServerResult>> mapTmp = Maps.newLinkedHashMap();
    for (RServerResult result : results) {
      if (result.isNamedList()) {
        RNamedList<RServerResult> namedResults = result.asNamedList();
        for (String name : namedResults.getNames()) {
          if (!mapTmp.containsKey(name)) {
            mapTmp.put(name, Lists.newArrayList());
          }
          mapTmp.get(name).add(namedResults.get(name));
        }
      }
    }
    for (String name : mapTmp.keySet()) {
      map.put(name, new RockResult(mapTmp.get(name)));
    }
  }

  @Override
  public List<String> getNames() {
    return Lists.newArrayList(map.keySet());
  }

  @Override
  public List<Map<String, Object>> asRows() {
    List<Map<String, Object>> rows = Lists.newArrayList();
    if (map.isEmpty()) return rows;

    List<String> names = getNames();
    var numRows = map.get(names.get(0)).length();
    for (int rowNum = 0; rowNum < numRows; rowNum++) {
      Map<String, Object> converted = new LinkedHashMap<>();
      rows.add(converted);
      for (String name : names) {
        RServerResult values = map.get(name);
        getValueAtIndex(values, rowNum).ifPresent(value -> converted.put(name, value));
      }
    }
    return rows;
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public RServerResult get(Object key) {
    return map.get(key);
  }

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public Collection<RServerResult> values() {
    return map.values();
  }

  @Override
  public Set<Entry<String, RServerResult>> entrySet() {
    return map.entrySet();
  }

  private Optional<Object> getValueAtIndex(RServerResult values, int rowNum) {
    if (values.isNA()[rowNum]) {
      return Optional.empty();
    }
    if (values.isInteger()) {
      return Optional.of(values.asIntegers()[rowNum]);
    } else if (values.isLogical()) {
      return Optional.of(values.asIntegers()[rowNum]).map(it -> Integer.valueOf(1).equals(it));
    } else if (values.isNumeric()) {
      return Optional.of(values.asDoubles()[rowNum]);
    } else if (values.isString()) {
      return Optional.ofNullable(values.asStrings()[rowNum]);
    } else {
      return Optional.empty();
    }
  }
}
