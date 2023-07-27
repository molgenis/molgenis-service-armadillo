package org.molgenis.r;

@FunctionalInterface
public interface RConnectionConsumer<T> {
  T accept(RServerConnection connection) throws RServerException;
}
