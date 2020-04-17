package org.molgenis.datashield;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import javax.annotation.PreDestroy;
import org.molgenis.datashield.exceptions.DataShieldSessionException;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataShieldSession {
  private RSession rSession = null;

  private static final Logger logger = LoggerFactory.getLogger(DataShieldSession.class);

  private final DataShieldConnectionFactory connectionFactory;

  public DataShieldSession(DataShieldConnectionFactory connectionFactory) {
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
      throw new DataShieldSessionException("Could not attach connection to RSession", err);
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
      throw new DataShieldSessionException("Closing session and/or connection failed", err);
    }
  }
}
