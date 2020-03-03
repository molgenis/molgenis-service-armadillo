package org.molgenis.datashield.service;

import org.rosuda.REngine.Rserve.RConnection;

public interface DataShieldConnectionFactory {
  RConnection createConnection();
}
