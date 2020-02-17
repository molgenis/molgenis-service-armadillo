package org.molgenis.datashield.r;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

public class RExecutorServiceImpl
{

	public String exec(String cmd, RDatashieldSession session) throws RserveException, REXPMismatchException
	{
		REXP value = session.execute(connection -> connection.eval(cmd));
		return value.asString();
	}
}
