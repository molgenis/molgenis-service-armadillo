package org.molgenis.r;

import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RConnectionFactoryImpl implements RConnectionFactory {

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  private final RConfigProperties rConfigProperties;

  public RConnectionFactoryImpl(RConfigProperties rConfigProperties) {
    this.rConfigProperties = rConfigProperties;
  }

  @Override
  public RConnection retryCreateConnection() {
    logger.debug("retryCreateConnection");
    return createConnection();
  }

  @Override
  public RConnection createConnection() {
    try {
      return newConnection(rConfigProperties.getHost(), rConfigProperties.getPort());
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }
}
