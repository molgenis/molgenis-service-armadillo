package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("ArmadilloSession")
public class ArmadilloSession {
  private static final Logger logger = LoggerFactory.getLogger(ArmadilloSession.class);

  private final ArmadilloConnectionFactory connectionFactory;
  private final ProcessService processService;
  private final RConnection connection;
  final int pid;
  private boolean busy = false;

  public ArmadilloSession(
      ArmadilloConnectionFactory connectionFactory, ProcessService processService) {
    this.connectionFactory = requireNonNull(connectionFactory);
    this.processService = requireNonNull(processService);
    logger.trace("Creating new connection...");
    connection = this.connectionFactory.createConnection();
    pid = this.processService.getPid(connection);
    logger.debug("Created new connection with pid {}.", pid);
  }

  public synchronized <T> T execute(Function<RConnection, T> consumer) {
    busy = true;
    try {
      return consumer.apply(connection);
    } finally {
      busy = false;
    }
  }

  public void sessionCleanup() {
    if (busy) {
      killProcess();
    }
    connection.close();
  }

  private void killProcess() {
    var conn = connectionFactory.createConnection();
    try {
      processService.terminateProcess(conn, pid);
    } finally {
      conn.close();
    }
  }
}
