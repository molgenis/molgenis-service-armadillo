package org.molgenis.datashield.r;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.rosuda.REngine.Rserve.protocol.RPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RConnectionFactoryImpl implements RConnectionFactory
{

	private static final int RSERVE_PORT = 6311;
	private static final String RSERVE_HOST = "localhost";
	private static final long START_ATTEMPT_SLEEP = 1000l;
	private static final int START_ATTEMP_COUNT = 5;

	private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

	@Override
	public RConnection getNewConnection(boolean enableBatchStart) throws RserveException
	{
		logger.debug("New connection using batch " + enableBatchStart + " at host:port [ " + RSERVE_HOST + ":" + RSERVE_PORT + " ]");

		RConnection con = null;
		try {
			con = newConnection(RSERVE_HOST, RSERVE_PORT);
		}
		catch (RserveException rse) {
			logger.debug("Could not connect to RServe: " + rse.getMessage());

			if (rse.getMessage().startsWith("Cannot connect") && enableBatchStart) {
				logger.info("Attempting to start RServe.");

				try {
					con = attemptStarts(RSERVE_HOST, RSERVE_PORT);
				}
				catch (Exception e) {
					logger.error("Attempted to start RServe and establish a connection failed", e);
				}
			}
			else
				throw rse;
		}

		return con;
	}

	private RConnection attemptStarts(String host, int port) throws InterruptedException, IOException, RserveException {
		int attempt = 1;
		RConnection con = null;
		while (attempt <= START_ATTEMP_COUNT) {
			try {
				Thread.sleep(START_ATTEMPT_SLEEP); // wait for R to startup, then establish connection
				con = newConnection(host, port);
				break;
			}
			catch (RserveException rse) {
				if (attempt >= 5) {
					throw rse;
				}

				attempt++;
			}
		}
		return con;
	}

	private static RConnection newConnection(String host, int port) throws RserveException {
		logger.debug("Creating new RConnection");

		RConnection con = new RConnection(host, port);

		REXP sessionInfo = con.eval("capture.output(sessionInfo())");
		try {
			logger.info("NEW CONNECTION >>> sessionInfo:\n" + Arrays.deepToString(sessionInfo.asStrings()));
		}
		catch (REXPMismatchException e) {
			logger.warn("Error creating session info.", e);
		}
		return con;
	}

}

