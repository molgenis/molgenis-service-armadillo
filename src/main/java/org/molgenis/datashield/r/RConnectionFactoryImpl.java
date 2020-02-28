package org.molgenis.datashield.r;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RConnectionFactoryImpl implements RConnectionFactory {

  @Value("${rserve.port}")
  private int rservePort;

  @Value("${rserve.host}")
  private String rserveHost;

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  @Override
  public RConnection getNewConnection(boolean enableBatchStart) throws RserveException {
    logger.debug(
        "New connection using batch {} at host:port [{}:{}]",
        enableBatchStart,
        rserveHost,
        rservePort);

    return enableBatchStart ? retryNewConnection() : newConnection();
  }

  @Retryable(
      exceptionExpression = "message.startsWith('Cannot connect')",
      value = {RserveException.class},
      maxAttempts = 5,
      backoff = @Backoff(delay = 1000))
  protected RConnection retryNewConnection() throws RserveException {
    logger.info("retryNewConnection");
    return newConnection();
  }

  private RConnection newConnection() throws RserveException {
    RConnection con = new RConnection(rserveHost, rservePort);
    REXP rSessionInfo = con.eval("capture.output(sessionInfo())");
    try {
      logger.info("New connection\n{}", String.join("\n", rSessionInfo.asStrings()));
    } catch (REXPMismatchException e) {
      logger.warn("Error creating session info.", e);
    }
    return con;
  }
}
