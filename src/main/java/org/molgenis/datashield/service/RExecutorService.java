package org.molgenis.datashield.service;

import org.molgenis.datashield.service.model.Table;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import java.io.IOException;
import java.io.InputStream;

public interface RExecutorService
{
	String execute(String cmd, RConnection connection)
		throws RserveException, REXPMismatchException;

	String assign(InputStream csv, Table table, RConnection connection)
			throws IOException, RserveException;
}
