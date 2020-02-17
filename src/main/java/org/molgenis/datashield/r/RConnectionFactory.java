package org.molgenis.datashield.r;

import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.lang.reflect.InvocationTargetException;

public interface RConnectionFactory
{
	RConnection getNewConnection(boolean enableBatchStart) throws RserveException;
}
