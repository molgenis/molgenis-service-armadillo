package org.molgenis.armadillo.service;

import org.molgenis.connection.RServerConnection;

public interface ArmadilloConnectionFactory {
  RServerConnection createConnection();
}
