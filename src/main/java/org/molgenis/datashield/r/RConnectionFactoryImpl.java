package org.molgenis.datashield.r;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RConnectionFactoryImpl implements RConnectionFactory {

  @Value("${rserve.port}")
  private int rservePort;

  @Value("${rserve.host}")
  private String rserveHost;

  @Value("${rserve.connect.interval}")
  private long startAttemptSleep;

  @Value("${rserve.connect.attempts}")
  private int startAttemptCount = 5;

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  @Override
  public RConnection getNewConnection(boolean enableBatchStart) throws RserveException {
    logger.debug(
        "New connection using batch {} at host:port [{}:{}]",
        enableBatchStart,
        rserveHost,
        rservePort);

    RConnection con = null;
    try {
      con = newConnection(rserveHost, rservePort);
    } catch (RserveException rse) {
      logger.debug("Could not connect to RServe: {}", rse.getMessage());

      if (rse.getMessage().startsWith("Cannot connect") && enableBatchStart) {
        logger.info("Attempting to start RServe.");

        try {
          con = attemptStarts(rserveHost, rservePort);
        } catch (Exception e) {
          logger.error("Attempted to start RServe and establish a connection failed", e);
        }
      } else throw rse;
    }

    return con;
  }

  private RConnection attemptStarts(String host, int port)
      throws InterruptedException, RserveException {
    int attempt = 1;
    RConnection con = null;
    while (attempt <= startAttemptCount) {
      try {
        Thread.sleep(startAttemptSleep); // wait for R to startup, then establish connection
        con = newConnection(host, port);
        break;
      } catch (RserveException rse) {
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

    REXP rSessionInfo = con.eval("capture.output(sessionInfo())");
    try {
      logger.info("New connection\n{}", String.join("\n", rSessionInfo.asStrings()));
    } catch (REXPMismatchException e) {
      logger.warn("Error creating session info.", e);
    }
    return con;
  }
}
