package org.molgenis.datashield.command.impl;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.molgenis.datashield.DataShieldSession;
import org.molgenis.datashield.command.Commands;
import org.molgenis.datashield.command.DataShieldCommand;
import org.molgenis.datashield.command.DataShieldCommandDTO;
import org.molgenis.datashield.model.Workspace;
import org.molgenis.datashield.service.DataShieldConnectionFactory;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
class CommandsImpl implements Commands {
  private final StorageService storageService;
  private final PackageService packageService;
  private final RExecutorService rExecutorService;
  private final DataShieldSession dataShieldSession;
  private final ExecutorService executorService;

  @SuppressWarnings("java:S3077") // DataShieldCommand is thread-safe
  private volatile DataShieldCommand lastCommand;

  public CommandsImpl(
      StorageService storageService,
      PackageService packageService,
      RExecutorService rExecutorService,
      ExecutorService executorService,
      DataShieldConnectionFactory connectionFactory) {
    this.storageService = storageService;
    this.packageService = packageService;
    this.rExecutorService = rExecutorService;
    this.dataShieldSession = new DataShieldSession(connectionFactory);
    this.executorService = executorService;
  }

  @Override
  public Optional<CompletableFuture<REXP>> getLastExecution() {
    return Optional.ofNullable(lastCommand).flatMap(it -> it.getExecution());
  }

  @Override
  public Optional<DataShieldCommandDTO> getLastCommand() {
    return Optional.ofNullable(lastCommand).map(DataShieldCommand::asDto);
  }

  synchronized <T> CompletableFuture<T> schedule(DataShieldCommandImpl<T> command) {
    final DataShieldSession session = dataShieldSession;
    lastCommand = command;
    CompletableFuture<T> result =
        supplyAsync(() -> session.execute(command::evaluate), executorService);
    command.setExecution(result);
    return result;
  }

  @Override
  public CompletableFuture<REXP> evaluate(String expression) {
    return schedule(
        new DataShieldCommandImpl<>(expression, true) {
          @Override
          protected REXP doWithConnection(RConnection connection) {
            return rExecutorService.execute(expression, connection);
          }
        });
  }

  @Override
  public CompletableFuture<Void> assign(String symbol, String expression) {
    String statement = format("%s <- %s", symbol, expression);
    return schedule(
        new DataShieldCommandImpl<>(statement, true) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            rExecutorService.execute(statement, connection);
            return null;
          }
        });
  }

  @Override
  public List<Workspace> listWorkspaces(String prefix) {
    return storageService.listWorkspaces(prefix);
  }

  @Override
  public CompletableFuture<Void> loadWorkspace(String objectName, String environment) {
    return schedule(
        new DataShieldCommandImpl<>("Load " + objectName, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            InputStream inputStream = storageService.load(objectName);
            rExecutorService.loadWorkspace(
                connection, new InputStreamResource(inputStream), environment);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> saveWorkspace(String objectname) {
    return schedule(
        new DataShieldCommandImpl<>("Save " + objectname, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            rExecutorService.saveWorkspace(
                connection, is -> storageService.save(is, objectname, APPLICATION_OCTET_STREAM));
            return null;
          }
        });
  }

  @Override
  public void removeWorkspace(String objectname) {
    storageService.delete(objectname);
  }

  @Override
  public CompletableFuture<List<RPackage>> getPackages() {
    return schedule(
        new DataShieldCommandImpl<>("getInstalledPackages", true) {
          @Override
          protected List<RPackage> doWithConnection(RConnection connection) {
            return packageService.getInstalledPackages(connection);
          }
        });
  }
}
