package org.molgenis.datashield.r;

import org.molgenis.datashield.exceptions.DatashieldNoSessionException;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import javax.annotation.PreDestroy;

@Component
@SessionScope
public class RDatashieldSession {
  private RSession rSession = null;

  private static final Logger logger = LoggerFactory.getLogger(RDatashieldSession.class);

  private final RConnectionFactory rConnectionFactory;

  public RDatashieldSession(RConnectionFactory rConnectionFactory) {
    this.rConnectionFactory = rConnectionFactory;
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
        return rConnectionFactory.getNewConnection(false);
      }
      return rSession.attach();
    } catch (RserveException err) {
      throw new RuntimeException("foutje", err);
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
      throw new DatashieldNoSessionException("Closing session and/or connection failed", err);
    }
  }
}
