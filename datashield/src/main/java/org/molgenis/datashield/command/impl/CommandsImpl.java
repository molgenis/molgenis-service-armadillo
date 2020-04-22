package org.molgenis.datashield.command.impl;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.molgenis.datashield.DataShieldUtils.GLOBAL_ENV;
import static org.molgenis.datashield.DataShieldUtils.TABLE_ENV;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
class CommandsImpl implements Commands {

  private final StorageService userStorageService;
  private final StorageService sharedStorageService;
  private final PackageService packageService;
  private final RExecutorService rExecutorService;
  private final DataShieldSession dataShieldSession;
  private final ExecutorService executorService;

  @SuppressWarnings("java:S3077") // DataShieldCommand is thread-safe
  private volatile DataShieldCommand lastCommand;

  public CommandsImpl(
      @Qualifier("userStorageService") StorageService userStorageService,
      @Qualifier("sharedStorageService") StorageService sharedStorageService,
      PackageService packageService,
      RExecutorService rExecutorService,
      ExecutorService executorService,
      DataShieldConnectionFactory connectionFactory) {
    this.sharedStorageService = sharedStorageService;
    this.userStorageService = userStorageService;
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
    return userStorageService.listWorkspaces(prefix);
  }

  @Override
  public CompletableFuture<List<String>> loadWorkspaces(List<String> objectNames) {

    return schedule(
        new DataShieldCommandImpl<>("Load " + objectNames, false) {
          @Override
          protected List<String> doWithConnection(RConnection connection) {
            objectNames.forEach(loadWorkspace(connection));
            return objectNames;
          }

          private Consumer<String> loadWorkspace(RConnection connection) {
            return objectName -> {
              InputStream inputStream = sharedStorageService.load(objectName);
              rExecutorService.loadWorkspace(
                  connection, new InputStreamResource(inputStream), TABLE_ENV);
            };
          }
        });
  }

  @Override
  public CompletableFuture<Void> loadUserWorkspace(String objectName) {
    return schedule(
        new DataShieldCommandImpl<>("Load " + objectName, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            InputStream inputStream = userStorageService.load(objectName);
            rExecutorService.loadWorkspace(
                connection, new InputStreamResource(inputStream), GLOBAL_ENV);
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
                connection,
                is -> userStorageService.save(is, objectname, APPLICATION_OCTET_STREAM));
            return null;
          }
        });
  }

  @Override
  public void removeWorkspace(String objectname) {
    userStorageService.delete(objectname);
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
