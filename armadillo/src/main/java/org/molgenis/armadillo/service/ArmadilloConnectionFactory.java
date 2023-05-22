package org.molgenis.armadillo.service;

import org.molgenis.r.RServerConnection;

public interface ArmadilloConnectionFactory {
  RServerConnection createConnection();
}
