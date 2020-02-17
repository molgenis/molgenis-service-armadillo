package org.molgenis.datashield.r;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public interface RConnectionFactory
{
  RConnection getNewConnection(boolean enableBatchStart) throws RserveException;
}
