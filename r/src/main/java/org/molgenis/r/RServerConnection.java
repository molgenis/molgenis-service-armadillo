package org.molgenis.r;

import java.io.InputStream;
import java.util.function.Consumer;

public interface RServerConnection {

  /**
   * Evaluate an expression and return the result object.
   *
   * @param expr
   * @return
   * @throws RServerException
   */
  default RServerResult eval(String expr) throws RServerException {
    return eval(expr, false);
  }

  /**
   * Evaluate an expression and return the result object.
   *
   * @param expr
   * @param serialized Result object is a raw one if true
   * @return
   * @throws RServerException
   */
  RServerResult eval(String expr, boolean serialized) throws RServerException;

  /**
   * Write a file from the input stream.
   *
   * @param fileName
   * @param in
   */
  void writeFile(String fileName, InputStream in) throws RServerException;

  /**
   * Read a file into the input stream consumer.
   *
   * @param fileName
   * @param inputStreamConsumer
   */
  void readFile(String fileName, Consumer<InputStream> inputStreamConsumer) throws RServerException;

  /** Close connection. */
  boolean close();
}
