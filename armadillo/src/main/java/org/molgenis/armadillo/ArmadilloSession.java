package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import javax.annotation.PreDestroy;
import org.molgenis.armadillo.exceptions.ArmadilloSessionException;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArmadilloSession {
  private RSession rSession = null;

  private static final Logger logger = LoggerFactory.getLogger(ArmadilloSession.class);

  private final ArmadilloConnectionFactory connectionFactory;

  public ArmadilloSession(ArmadilloConnectionFactory connectionFactory) {
    this.connectionFactory = requireNonNull(connectionFactory);
  }

  public synchronized <T> T execute(Function<RConnection, T> consumer) {
    RConnection connection = getRConnection();
    try {
      return consumer.apply(connection);
    } finally {
      tryDetachRConnection(connection);
    }
  }

  void tryDetachRConnection(RConnection connection) {
    try {
      rSession = connection.detach();
    } catch (RserveException e) {
      logger.error("Failed to detach connection", e);
      rSession = null;
    }
  }

  RConnection getRConnection() {
    try {
      if (rSession == null) {
        return connectionFactory.createConnection();
      }
      return rSession.attach();
    } catch (RserveException err) {
      throw new ArmadilloSessionException("Could not attach connection to RSession", err);
    }
  }

  @PreDestroy
  public synchronized void sessionCleanup() {
    try {
      if (rSession != null) {
        logger.debug("Cleanup session");
        RConnection connection = rSession.attach();
        connection.close();
      }
    } catch (RserveException err) {
      throw new ArmadilloSessionException("Closing session and/or connection failed", err);
    }
  }
}
