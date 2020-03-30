package org.molgenis.datashield;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.PreDestroy;
import org.molgenis.datashield.exceptions.DataShieldSessionException;
import org.molgenis.datashield.pojo.DataShieldCommand;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.r.RConnectionConsumer;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
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
  private final ExecutorService executorService;
  private final RExecutorService rExecutorService;

  private volatile DataShieldCommand<REXP> lastCommand;

  public DataShieldSession(
      DataShieldConnectionFactory connectionFactory,
      ExecutorService executorService,
      RExecutorService rExecutorService) {
    this.connectionFactory = requireNonNull(connectionFactory);
    this.executorService = requireNonNull(executorService);
    this.rExecutorService = requireNonNull(rExecutorService);
  }

  public DataShieldCommand<REXP> getLastCommand() {
    return lastCommand;
  }

  public synchronized DataShieldCommand<REXP> schedule(String command) {
    CompletableFuture<REXP> result =
        supplyAsync(
            () -> execute(connection -> rExecutorService.execute(command, connection)),
            executorService);
    lastCommand = new DataShieldCommand<>(result, command);
    return lastCommand;
  }

  public synchronized <T> T execute(RConnectionConsumer<T> consumer) {
    RConnection connection = getRConnection();
    try {
      return consumer.accept(connection);
    } catch (RserveException | REXPMismatchException e) {
      throw new RExecutionException(e);
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
