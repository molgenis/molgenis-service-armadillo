package org.molgenis.connection;

@FunctionalInterface
public interface RConnectionConsumer<T> {
  T accept(RServerConnection connection) throws RServerException;
}
