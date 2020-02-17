package org.molgenis.datashield.r;

import org.junit.jupiter.api.Test;
import org.molgenis.datashield.service.RExecutorServiceImpl;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

class RExecutorServiceImplTest
{

	private RConnectionFactory rConnectionFactory = new RConnectionFactoryImpl();

	private RExecutorServiceImpl rExecutorService = new RExecutorServiceImpl();

	@Test
	public void testExec() throws RserveException, REXPMismatchException
	{
		RDatashieldSession session = new RDatashieldSession();
		session.execute(connection -> {
			System.out.println("\n" + rExecutorService.exec("1+1", connection));
			System.out.println("\n" + rExecutorService.exec("3+3", connection));
			return null;
		});

	}


}