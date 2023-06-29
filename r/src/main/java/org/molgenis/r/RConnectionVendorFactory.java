package org.molgenis.r;

/**
 * An R connection factory for the specific vendors implementation, not to be confused with the
 * higher level {@link RConnectionFactory}, without the retry strategy.
 */
public interface RConnectionVendorFactory {
  RServerConnection tryCreateConnection();
}
