package org.molgenis.datashield.r;

import org.junit.jupiter.api.Test;
import org.rosuda.REngine.REXPMismatchException;

class RExecutorServiceImplTest
{
	RExecutorServiceImpl rExecutorService = new RExecutorServiceImpl();

	@Test
	public void testExec()
			throws REXPMismatchException
	{
		System.out.println("\n" + rExecutorService.exec("1+1"));
	}


}