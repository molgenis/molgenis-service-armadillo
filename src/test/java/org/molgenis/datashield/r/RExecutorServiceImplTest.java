package org.molgenis.datashield.r;

import org.junit.jupiter.api.Test;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.beans.factory.annotation.Autowired;

class RExecutorServiceImplTest
{



	private RConnectionFactory rConnectionFactory = new RConnectionFactoryImpl();

	private RExecutorServiceImpl rExecutorService = new RExecutorServiceImpl();

	@Test
	public void testExec() throws REXPMismatchException, RserveException, InterruptedException
	{
		RDatashieldSession session = new RDatashieldSession(rConnectionFactory);
		System.out.println("\n" + rExecutorService.exec("1+1", session));
		Thread.sleep(100);
		System.out.println("\n" + rExecutorService.exec("3+3", session));
	}


}