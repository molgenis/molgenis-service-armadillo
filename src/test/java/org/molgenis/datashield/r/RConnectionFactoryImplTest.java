package org.molgenis.datashield.r;

import org.junit.jupiter.api.Test;
import org.rosuda.REngine.Rserve.RserveException;

class RConnectionFactoryImplTest
{

	private RConnectionFactoryImpl connector = new RConnectionFactoryImpl();

	@Test
	public void testGetNewConnection() throws RserveException
	{
		connector.getNewConnection(false);
	}
}