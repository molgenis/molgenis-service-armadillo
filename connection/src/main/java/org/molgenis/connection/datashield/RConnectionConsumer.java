package org.molgenis.connection.datashield;

@FunctionalInterface
public interface RConnectionConsumer<T> {
  T accept(RServerConnection connection) throws RServerException;
}
