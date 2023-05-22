package org.molgenis.r;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RServerConnection {

  /**
   * Evaluate an expression and return the result object.
   *
   * @param expr
   * @return
   * @throws RServerException
   */
  RServerResult eval(String expr) throws RServerException;

  /**
   * Create a file output stream.
   *
   * @param fileName
   */
  OutputStream createFile(String fileName) throws IOException;

  /**
   * Open a file input stream.
   *
   * @param fileName
   */
  InputStream openFile(String fileName) throws IOException;

  /** Close connection. */
  boolean close();
}
