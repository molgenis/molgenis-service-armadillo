package org.molgenis.r;

import static java.util.Objects.requireNonNull;

import org.molgenis.r.config.Node;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RConnectionFactoryImpl implements RConnectionFactory {

  private static final Logger logger = LoggerFactory.getLogger(RConnectionFactoryImpl.class);

  private final Node node;

  public RConnectionFactoryImpl(Node node) {
    this.node = requireNonNull(node);
  }

  @Override
  public RConnection retryCreateConnection() {
    logger.debug("retryCreateConnection");
    return createConnection();
  }

  @Override
  public RConnection createConnection() {
    try {
      return newConnection(node.getHost(), node.getPort());
    } catch (RserveException ex) {
      throw new ConnectionCreationFailedException(ex);
    }
  }

  @Override
  public String getName() {
    return node.getName();
  }

  RConnection newConnection(String host, int port) throws RserveException {
    return new RConnection(host, port);
  }

  @Override
  public String toString() {
    return "RConnectionFactoryImpl{" + "profile=" + node.getName() + '}';
  }
}
