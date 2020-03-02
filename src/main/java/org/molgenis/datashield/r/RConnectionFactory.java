package org.molgenis.datashield.r;

import org.rosuda.REngine.Rserve.RConnection;

public interface RConnectionFactory {
  RConnection getNewConnection(boolean enableBatchStart);
}
