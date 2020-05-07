package org.molgenis.armadillo.service;

import org.rosuda.REngine.Rserve.RConnection;

public interface DataShieldConnectionFactory {
  RConnection createConnection();
}
