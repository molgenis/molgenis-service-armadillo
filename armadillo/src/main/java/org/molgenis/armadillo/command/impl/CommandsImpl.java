package org.molgenis.armadillo.command.impl;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static org.molgenis.armadillo.ArmadilloUtils.GLOBAL_ENV;
import static org.molgenis.armadillo.ArmadilloUtils.TABLE_ENV;
import static org.molgenis.armadillo.minio.TableService.SHARED_PREFIX;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import io.minio.messages.Item;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.PreDestroy;
import org.molgenis.armadillo.ArmadilloSession;
import org.molgenis.armadillo.command.ArmadilloCommand;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.minio.TableService;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.armadillo.service.StorageService;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
class CommandsImpl implements Commands {

  private static final String SAVE_PATTERN = format("^(?!%s).*", quote(TABLE_ENV));

  private final StorageService storageService;
  private final TableService tableService;
  private final PackageService packageService;
  private final RExecutorService rExecutorService;
  private final ArmadilloSession armadilloSession;
  private final ExecutorService executorService;

  @SuppressWarnings("java:S3077") // ArmadilloCommand is thread-safe
  private volatile ArmadilloCommand lastCommand;

  public CommandsImpl(
      StorageService storageService,
      TableService tableService,
      PackageService packageService,
      RExecutorService rExecutorService,
      ExecutorService executorService,
      ArmadilloConnectionFactory connectionFactory,
      ProcessService processService) {
    this.storageService = storageService;
    this.tableService = tableService;
    this.packageService = packageService;
    this.rExecutorService = rExecutorService;
    this.armadilloSession = new ArmadilloSession(connectionFactory, processService);
    this.executorService = executorService;
  }

  @Override
  public Optional<CompletableFuture<REXP>> getLastExecution() {
    return Optional.ofNullable(lastCommand).flatMap(it -> it.getExecution());
  }

  @Override
  public Optional<ArmadilloCommandDTO> getLastCommand() {
    return Optional.ofNullable(lastCommand).map(ArmadilloCommand::asDto);
  }

  synchronized <T> CompletableFuture<T> schedule(ArmadilloCommandImpl<T> command) {
    final ArmadilloSession session = armadilloSession;
    lastCommand = command;
    CompletableFuture<T> result =
        supplyAsync(() -> session.execute(command::evaluate), executorService);
    command.setExecution(result);
    return result;
  }

  @Override
  public CompletableFuture<REXP> evaluate(String expression) {
    return schedule(
        new ArmadilloCommandImpl<>(expression, true) {
          @Override
          protected REXP doWithConnection(RConnection connection) {
            return rExecutorService.execute(expression, connection);
          }
        });
  }

  @Override
  public CompletableFuture<Void> assign(String symbol, String expression) {
    String statement = format("is.null(base::assign('%s', value={%s}))", symbol, expression);
    return schedule(
        new ArmadilloCommandImpl<>(statement, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            rExecutorService.execute(statement, connection);
            return null;
          }
        });
  }

  @Override
  public List<Workspace> listWorkspaces(String bucketName) {
    return storageService.listObjects(bucketName).stream()
        .map(CommandsImpl::toWorkspace)
        .collect(toList());
  }

  public static Workspace toWorkspace(Item item) {
    return Workspace.builder()
        .setLastModified(item.lastModified())
        .setName(item.objectName())
        .setSize(item.objectSize())
        .setETag(item.etag())
        .build();
  }

  @Override
  public List<String> listSharedTables() {
    var stringStringMultimap = tableService.listTables();
    return stringStringMultimap.entries().stream()
        .map(it -> format("%s/%s", it.getKey().substring(SHARED_PREFIX.length()), it.getValue()))
        .collect(toList());
  }

  @Override
  public CompletableFuture<Void> loadUserWorkspace(String bucketName, String objectName) {
    return schedule(
        new ArmadilloCommandImpl<>("Load " + objectName, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            InputStream inputStream = storageService.load(bucketName, objectName);
            rExecutorService.loadWorkspace(
                connection, new InputStreamResource(inputStream), GLOBAL_ENV);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> loadTable(String symbol, String table, String variables) {
    return schedule(
        new ArmadilloCommandImpl<>("Load " + table, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            InputStream inputStream = tableService.loadTable(table);
            rExecutorService.loadTable(
                connection, new InputStreamResource(inputStream), table, symbol, variables);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> saveWorkspace(String bucketName, String objectname) {
    return schedule(
        new ArmadilloCommandImpl<>("Save " + objectname, false) {
          @Override
          protected Void doWithConnection(RConnection connection) {
            rExecutorService.saveWorkspace(
                SAVE_PATTERN,
                connection,
                is -> storageService.save(is, bucketName, objectname, APPLICATION_OCTET_STREAM));
            return null;
          }
        });
  }

  @Override
  public void removeWorkspace(String bucketName, String objectName) {
    storageService.delete(bucketName, objectName);
  }

  @Override
  public CompletableFuture<List<RPackage>> getPackages() {
    return schedule(
        new ArmadilloCommandImpl<>("getInstalledPackages", true) {
          @Override
          protected List<RPackage> doWithConnection(RConnection connection) {
            return packageService.getInstalledPackages(connection);
          }
        });
  }

  @PreDestroy
  public void preDestroy() {
    armadilloSession.sessionCleanup();
  }
}
