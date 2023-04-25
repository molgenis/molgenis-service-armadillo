package org.molgenis.r;

import java.util.List;

public interface RServerResult {

  /**
   * Get the length, if it makes sense for the type of data, returns -1 otherwise.
   *
   * @return
   */
  int length();

  /**
   * Get the evaluation result as an array of bytes.
   *
   * @return
   */
  byte[] asBytes();

  /**
   * Check if the evaluation result is an array of doubles.
   *
   * @return
   */
  boolean isNumeric();

  /**
   * Get the evaluation result as an array of doubles.
   *
   * @return
   */
  double[] asDoubles();

  /**
   * Check if the evaluation result is an array of integers.
   *
   * @return
   */
  boolean isInteger();

  /**
   * Get the evaluation result as an array of integers.
   *
   * @return
   */
  int[] asIntegers();

  /**
   * Get the first integer from the evaluation result as an array of integers.
   *
   * @return
   */
  int asInteger();

  /**
   * Check if the evaluation result is an array of logicals.
   *
   * @return
   */
  boolean isLogical();

  /**
   * Get the evaluation result as a single logical.
   *
   * @return
   */
  boolean asLogical();

  /**
   * Check wether the data is null or represents a null value.
   *
   * @return
   */
  boolean isNull();

  /**
   * Check if the evaluation result is an array of strings.
   *
   * @return
   */
  boolean isString();

  /**
   * Get the evaluation result as an array of strings.
   *
   * @return
   */
  String[] asStrings();

  /**
   * Check whether it is a list (named or not).
   *
   * @return
   */
  boolean isList();

  /**
   * Get the evaluation result list of values.
   *
   * @return
   */
  List<RServerResult> asList();

  /**
   * Check if there are names associated to a list.
   *
   * @return
   */
  boolean isNamedList();

  /**
   * Get the evaluation result as a named list of results.
   *
   * @return
   */
  RNamedList<RServerResult> asNamedList();

  /**
   * Get whether there are NAs in a vector of data.
   *
   * @return
   */
  boolean[] isNA();

  /**
   * Get the data as a Java object.
   *
   * @return
   */
  Object asNativeJavaObject();
}
