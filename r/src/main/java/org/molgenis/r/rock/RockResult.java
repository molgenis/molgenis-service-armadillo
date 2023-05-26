package org.molgenis.r.rock;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerResult;

public class RockResult implements RServerResult {

  private byte[] rawResult;

  private List<RServerResult> listResult;

  private RockNamedList namedListResult;

  private Object nativeResult;

  private String jsonResult;

  public RockResult(byte[] rawResult) {
    this.rawResult = rawResult;
  }

  public RockResult(String jsonSource) {
    if (jsonSource == null || jsonSource.equals("{}")) jsonResult = null;
    else {
      if (jsonSource.startsWith("{"))
        this.namedListResult = new RockNamedList(new JSONObject(jsonSource));
      else if (jsonSource.startsWith("[")) this.listResult = toList(new JSONArray(jsonSource));
      else this.listResult = toList(new JSONArray(String.format("[%s]", jsonSource)));
      this.jsonResult = jsonSource;
    }
  }

  public RockResult(JSONArray array) {
    this.listResult = toList(array);
    this.jsonResult = array.toString();
  }

  public RockResult(JSONObject object) {
    this.namedListResult = new RockNamedList(object);
    this.jsonResult = object.toString();
  }

  public RockResult(Object nativeResult) {
    this.nativeResult = nativeResult;
  }

  @Override
  public int length() {
    if (isRaw()) return rawResult.length;
    if (isList()) return listResult.size();
    if (isNamedList()) return namedListResult.size();
    if (nativeResult != null) return 1;
    return -1;
  }

  @Override
  public byte[] asBytes() {
    if (isRaw()) return rawResult;
    if (isString()) return nativeResult.toString().getBytes();
    return null;
  }

  @Override
  public boolean isNumeric() {
    if (isList()) return listResult.stream().allMatch(RServerResult::isNumeric);
    if (isNamedList()) return namedListResult.values().stream().allMatch(RServerResult::isNumeric);
    if (nativeResult != null) {
      if (nativeResult instanceof String) return false;
      try {
        Double.parseDouble(nativeResult.toString());
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  @Override
  public double[] asDoubles() {
    if (isList()) {
      double[] rval = new double[listResult.size()];
      for (int i = 0; i < listResult.size(); i++) {
        rval[i] = listResult.get(i).asDoubles()[0];
      }
      return rval;
    }
    if (isNamedList()) {
      double[] rval = new double[namedListResult.size()];
      int i = 0;
      for (RServerResult value : namedListResult.values()) {
        rval[i++] = value.asDoubles()[0];
      }
      return rval;
    }
    if (nativeResult != null) {
      try {
        return new double[] {Double.parseDouble(nativeResult.toString())};
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public boolean isInteger() {
    if (isList()) return listResult.stream().allMatch(RServerResult::isInteger);
    if (isNamedList()) return namedListResult.values().stream().allMatch(RServerResult::isInteger);
    if (nativeResult != null) {
      if (nativeResult instanceof String) return false;
      try {
        Integer.parseInt(nativeResult.toString());
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  @Override
  public int[] asIntegers() {
    if (isList()) {
      int[] rval = new int[listResult.size()];
      for (int i = 0; i < listResult.size(); i++) {
        rval[i] = listResult.get(i).asIntegers()[0];
      }
      return rval;
    }
    if (isNamedList()) {
      int[] rval = new int[namedListResult.size()];
      int i = 0;
      for (RServerResult value : namedListResult.values()) {
        rval[i++] = value.asIntegers()[0];
      }
      return rval;
    }
    if (nativeResult != null) {
      try {
        return new int[] {Integer.parseInt(nativeResult.toString())};
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public int asInteger() {
    int[] values = asIntegers();
    return values != null ? values[0] : 0;
  }

  @Override
  public boolean isLogical() {
    if (isList()) return listResult.stream().allMatch(RServerResult::isLogical);
    if (isNamedList()) return namedListResult.values().stream().allMatch(RServerResult::isLogical);
    if (nativeResult != null) {
      if (nativeResult instanceof String) return false;
      try {
        Boolean.parseBoolean(nativeResult.toString());
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  @Override
  public boolean asLogical() {
    Boolean[] values = asLogicals();
    return values != null && values[0];
  }

  @Override
  public boolean isNull() {
    Object objValue = asNativeJavaObject();
    return objValue == null || JSONObject.NULL.equals(objValue) || objValue.toString().equals("NA");
  }

  @Override
  public boolean isString() {
    return isList() || isNamedList() || jsonResult != null || nativeResult != null;
  }

  @Override
  public String[] asStrings() {
    if (isList()) {
      String[] rval = new String[listResult.size()];
      for (int i = 0; i < listResult.size(); i++) {
        rval[i] = listResult.get(i).asStrings()[0];
      }
      return rval;
    }
    if (isNamedList()) {
      String[] rval = new String[namedListResult.size()];
      int i = 0;
      for (RServerResult value : namedListResult.values()) {
        rval[i++] = value.asStrings()[0];
      }
      return rval;
    }
    if (jsonResult != null) {
      try {
        return new String[] {jsonResult};
      } catch (Exception e) {
        return null;
      }
    }
    if (nativeResult != null) {
      try {
        return new String[] {nativeResult.toString()};
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  @Override
  public boolean isList() {
    return listResult != null;
  }

  @Override
  public List<RServerResult> asList() {
    return listResult;
  }

  @Override
  public boolean isNamedList() {
    return namedListResult != null;
  }

  @Override
  public RNamedList<RServerResult> asNamedList() {
    return namedListResult;
  }

  @Override
  public boolean[] isNA() {
    return new boolean[0];
  }

  @Override
  public Object asNativeJavaObject() {
    if (isRaw()) return rawResult;
    if (isList())
      return listResult.stream()
          .map(RServerResult::asNativeJavaObject)
          .collect(Collectors.toList());
    if (isNamedList())
      return namedListResult.keySet().stream()
          .collect(
              Collectors.toMap(
                  Function.identity(), k -> namedListResult.get(k).asNativeJavaObject()));
    return nativeResult;
  }

  private Boolean[] asLogicals() {
    if (isList()) {
      Boolean[] rval = new Boolean[listResult.size()];
      for (int i = 0; i < listResult.size(); i++) {
        rval[i] = listResult.get(i).asLogical();
      }
      return rval;
    }
    if (isNamedList()) {
      Boolean[] rval = new Boolean[namedListResult.size()];
      int i = 0;
      for (RServerResult value : namedListResult.values()) {
        rval[i++] = value.asLogical();
      }
      return rval;
    }
    if (jsonResult != null) {
      try {
        return new Boolean[] {Boolean.parseBoolean(jsonResult)};
      } catch (Exception e) {
        return null;
      }
    }
    if (nativeResult != null) {
      try {
        return new Boolean[] {Boolean.parseBoolean(nativeResult.toString())};
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private boolean isRaw() {
    return rawResult != null;
  }

  private List<RServerResult> toList(JSONArray array) {
    List<RServerResult> list = Lists.newArrayList();
    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);
      if (value instanceof JSONArray) {
        list.add(new RockResult((JSONArray) value));
      } else if (value instanceof JSONObject) {
        list.add(new RockResult((JSONObject) value));
      } else {
        list.add(new RockResult(value));
      }
    }
    return list;
  }
}
