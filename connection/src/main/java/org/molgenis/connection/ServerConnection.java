package org.molgenis.connection;

import java.io.InputStream;
import java.util.function.Consumer;

public interface ServerConnection {

  ServerResult eval(String expr) throws Exception;

  /**
   * Write a file from the input stream.
   *
   * @param fileName
   * @param in
   */
  void writeFile(String fileName, InputStream in) throws Exception;

  /**
   * Read a file into the input stream consumer.
   *
   * @param fileName
   * @param inputStreamConsumer
   */
  void readFile(String fileName, Consumer<InputStream> inputStreamConsumer) throws Exception;

  /** Close connection. */
  boolean close();
}
