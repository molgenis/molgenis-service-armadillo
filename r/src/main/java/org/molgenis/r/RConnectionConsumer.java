package org.molgenis.r;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

@FunctionalInterface
public interface RConnectionConsumer<T> {
  T accept(RConnection connection) throws RserveException, REXPMismatchException;
}
