package org.molgenis.armadillo.service;

import org.molgenis.connection.datashield.RServerConnection;

public interface ArmadilloConnectionFactory {
  RServerConnection createConnection();
}
