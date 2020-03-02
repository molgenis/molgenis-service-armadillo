package org.molgenis.datashield.r;

import java.util.Map.Entry;
import org.molgenis.datashield.exceptions.ConnectionCreationFailedException;
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
  private final DataShieldOptions dataShieldOptions;

  public RConnectionFactoryImpl(RConfigProperties rConfigProperties, DataShieldOptions options) {
    this.rConfigProperties = rConfigProperties;
    this.dataShieldOptions = options;
  }

  @Override
  public RConnection getNewConnection(boolean enableBatchStart) {
    logger.debug(
        "New connection using batch {} at host:port [{}:{}]",
        enableBatchStart,
        rConfigProperties.getHost(),
        rConfigProperties.getPort());

    return enableBatchStart ? retryNewConnection() : getNewConnection();
  }

  @Retryable(
      value = {ConnectionCreationFailedException.class},
      maxAttempts = 5,
      backoff = @Backoff(delay = 1000))
  protected RConnection retryNewConnection() {
    logger.info("retryNewConnection");
    return getNewConnection();
  }

  RConnection getNewConnection() {
    try {
      RConnection con = newConnection(rConfigProperties.getHost(), rConfigProperties.getPort());
      setDataShieldOptions(con);
      logSessionInfo(con);
      return con;
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }

  void setDataShieldOptions(RConnection con) throws RserveException {
    for (Entry<String, String> option : dataShieldOptions.getValue().entrySet()) {
      con.eval(String.format("options(%s = %s)", option.getKey(), option.getValue()));
    }
  }

  private void logSessionInfo(RConnection con) throws RserveException {
    if (logger.isDebugEnabled()) {
      REXP rSessionInfo = con.eval("capture.output(sessionInfo())");
      try {
        logger.debug("New connection\n{}", String.join("\n", rSessionInfo.asStrings()));
      } catch (REXPMismatchException e) {
        logger.warn("Error parsing session info.", e);
      }
    }
  }
}
