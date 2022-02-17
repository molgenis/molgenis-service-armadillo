package org.molgenis.r;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RConnectionFactoryImpl implements RConnectionFactory {

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  private final EnvironmentConfigProps environment;

  public RConnectionFactoryImpl(EnvironmentConfigProps environment) {
    this.environment = requireNonNull(environment);
  }

  @Override
  public RConnection tryCreateConnection() {
    logger.debug(
        format("Trying to connect to instance %s on %s", environment.getHost(), environment.getPort()));
    try {
      return newConnection(environment.getHost(), environment.getPort());
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }

  @Override
  public String toString() {
    return "RConnectionFactoryImpl{" + "environment=" + environment.getName() + '}';
  }
}
