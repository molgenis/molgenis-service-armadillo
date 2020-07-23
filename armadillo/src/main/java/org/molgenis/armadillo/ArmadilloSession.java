package org.molgenis.armadillo;

import static java.lang.System.exit;
import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import org.molgenis.armadillo.exceptions.ArmadilloSessionException;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class ArmadilloSession {
  private RSession rSession = null;

  private static final Logger logger = LoggerFactory.getLogger(ArmadilloSession.class);

  private final ArmadilloConnectionFactory connectionFactory;
  private final RetryTemplate retryTemplate;

  public ArmadilloSession(ArmadilloConnectionFactory connectionFactory) {
    this.connectionFactory = requireNonNull(connectionFactory);
    retryTemplate = new RetryTemplate();
    retryTemplate.setThrowLastExceptionOnExhausted(true);

    var fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(1000);
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    var retryPolicy = new SimpleRetryPolicy(3);
    retryTemplate.setRetryPolicy(retryPolicy);
  }

  public synchronized <T> T execute(Function<RConnection, T> consumer) {
    RConnection connection = getRConnection();
    try {
      return consumer.apply(connection);
    } finally {
      retryDetachRConnection(connection);
    }
  }

  void retryDetachRConnection(@NotNull RConnection connection) {
    try {
      retryTemplate.execute(context -> {
        try {
          tryDetachRConnection(connection);
        } catch (RserveException ex) {
          logger.warn("Failed to detach connection. retryCount={}", context.getRetryCount());
          throw ex;
        }
        return null;
      });
    } catch (RserveException e) {
      logger.error("Failed to detach connection after retrying", e);
      connection.close();
    }
  }

  void tryDetachRConnection(RConnection connection) throws RserveException {
    rSession = connection.detach();
  }

  RConnection getRConnection() {
    try {
      if (rSession == null) {
        return connectionFactory.createConnection();
      }
      return rSession.attach();
    } catch (RserveException err) {
      if(rSession == null) {
        logger.error("Error creating connection", err);
      }
      logger.error("Error attaching connection", err);
      throw new ArmadilloSessionException("Could not attach connection to RSession", err);
    }
  }

  public synchronized void sessionCleanup() {
    try {
      if (rSession != null) {
        logger.debug("Cleanup session");
        RConnection connection = rSession.attach();
        connection.close();
      }
    } catch (RserveException err) {
      logger.error("Closing session and/or connection failed", err);
      throw new ArmadilloSessionException("Closing session and/or connection failed", err);
    }
  }
}
