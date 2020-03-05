package org.molgenis.r;

import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RConnectionFactoryImpl implements RConnectionFactory {

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  private final RConfigProperties rConfigProperties;

  public RConnectionFactoryImpl(RConfigProperties rConfigProperties) {
    this.rConfigProperties = rConfigProperties;
  }

  @Override
  @Retryable(
      value = {ConnectionCreationFailedException.class},
      maxAttempts = 5,
      backoff = @Backoff(delay = 1000))
  public RConnection retryCreateConnection() {
    logger.info("retryCreateConnection");
    return createConnection();
  }

  @Override
  public RConnection createConnection() {
    try {
      RConnection con = newConnection(rConfigProperties.getHost(), rConfigProperties.getPort());
      logSessionInfo(con);
      return con;
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }

  private void logSessionInfo(RConnection con) throws RserveException {
    if (logger.isDebugEnabled()) {
      REXP rSessionInfo = con.eval("capture.output(sessionInfo())");
      try {
        logger.debug("Session info:\n{}", String.join("\n", rSessionInfo.asStrings()));
      } catch (REXPMismatchException e) {
        logger.warn("Error parsing session info.", e);
      }
    }
  }
}
