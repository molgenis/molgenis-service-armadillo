package org.molgenis.datashield;

import javax.annotation.PreDestroy;
import org.molgenis.datashield.exceptions.DataShieldSessionException;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.r.RConnectionConsumer;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class DataShieldSession {
  private RSession rSession = null;

  private static final Logger logger = LoggerFactory.getLogger(DataShieldSession.class);

  private final DataShieldConnectionFactory connectionFactory;

  public DataShieldSession(DataShieldConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public synchronized <T> T execute(RConnectionConsumer<T> consumer)
      throws RserveException, REXPMismatchException {
    RConnection connection = getRConnection();
    try {
      return consumer.accept(connection);
    } finally {
      rSession = connection.detach();
    }
  }

  private RConnection getRConnection() {
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
