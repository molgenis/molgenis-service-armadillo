package org.molgenis.armadillo.command.impl;

import static java.lang.String.format;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.molgenis.armadillo.controller.ArmadilloUtils.GLOBAL_ENV;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.PARQUET;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.RDS;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.molgenis.armadillo.ArmadilloSession;
import org.molgenis.armadillo.command.ArmadilloCommand;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.container.ActiveContainerNameAccessor;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.armadillo.security.ResourceTokenService;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerResult;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.ProcessService;
import org.molgenis.r.service.RExecutorService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
class CommandsImpl implements Commands {

  private final ArmadilloStorageService armadilloStorage;
  private final PackageService packageService;
  private final RExecutorService rExecutorService;
  private final TaskExecutor taskExecutor;
  private final ArmadilloConnectionFactory connectionFactory;
  private final ProcessService processService;
  private final ContainerService containerService;
  private final ResourceTokenService resourceTokenService;

  private ArmadilloSession armadilloSession;

  @SuppressWarnings("java:S3077") // ArmadilloCommand is thread-safe
  private volatile ArmadilloCommand lastCommand;

  public CommandsImpl(
      ArmadilloStorageService armadilloStorage,
      PackageService packageService,
      RExecutorService rExecutorService,
      TaskExecutor taskExecutor,
      ArmadilloConnectionFactory connectionFactory,
      ProcessService processService,
      ContainerService containerService,
      ResourceTokenService resourceTokenService) {
    this.armadilloStorage = armadilloStorage;
    this.packageService = packageService;
    this.rExecutorService = rExecutorService;
    this.taskExecutor = taskExecutor;
    this.connectionFactory = connectionFactory;
    this.processService = processService;
    this.containerService = containerService;
    this.resourceTokenService = resourceTokenService;
  }

  @Override
  public String getActiveContainerName() {
    return ActiveContainerNameAccessor.getActiveContainerName();
  }

  @Override
  public void selectContainer(String containerName) {
    runAsSystem(() -> containerService.getByName(containerName));
    if (armadilloSession != null) armadilloSession.sessionCleanup();
    ActiveContainerNameAccessor.setActiveContainerName(containerName);
    armadilloSession = new ArmadilloSession(connectionFactory, processService);
  }

  @Override
  public List<String> listContainers() {
    return runAsSystem(
        () -> containerService.getAll().stream().map(ContainerConfig::getName).toList());
  }

  @Override
  public Optional getLastExecution() {
    return Optional.ofNullable(lastCommand).flatMap(ArmadilloCommand::getExecution);
  }

  @Override
  public Optional<ArmadilloCommandDTO> getLastCommand() {
    return Optional.ofNullable(lastCommand).map(ArmadilloCommand::asDto);
  }

  synchronized <T> CompletableFuture<T> schedule(ArmadilloCommandImpl<T> command) {
    final ArmadilloSession session =
        armadilloSession == null
            ? new ArmadilloSession(connectionFactory, processService)
            : armadilloSession;
    lastCommand = command;
    CompletableFuture<T> result =
        supplyAsync(() -> session.execute(command::evaluate), taskExecutor);
    command.setExecution(result);
    return result;
  }

  @Override
  public CompletableFuture<RServerResult> evaluate(String expression) {
    return evaluate(expression, false);
  }

  @Override
  public CompletableFuture<RServerResult> evaluate(String expression, boolean serialized) {
    return schedule(
        new ArmadilloCommandImpl<>(expression, true) {
          @Override
          protected RServerResult doWithConnection(RServerConnection connection) {
            return rExecutorService.execute(expression, serialized, connection);
          }
        });
  }

  @Override
  public CompletableFuture<Void> assign(String symbol, String expression) {
    String statement = format("is.null(base::assign('%s', value={%s}))", symbol, expression);
    return schedule(
        new ArmadilloCommandImpl<>(statement, false) {
          @Override
          protected Void doWithConnection(RServerConnection connection) {
            rExecutorService.execute(statement, connection);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> loadWorkspace(Principal principal, String id) {
    return schedule(
        new ArmadilloCommandImpl<>("Load user workspace " + id, false) {
          @Override
          protected Void doWithConnection(RServerConnection connection) {
            InputStream inputStream = armadilloStorage.loadWorkspace(principal, id);
            rExecutorService.loadWorkspace(
                connection, new InputStreamResource(inputStream), GLOBAL_ENV);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> loadTable(String symbol, String table, List<String> variables) {
    int index = table.indexOf('/');
    String project = table.substring(0, index);
    String objectName = table.substring(index + 1);
    return schedule(
        new ArmadilloCommandImpl<>("Load table " + table, false) {
          @Override
          protected Void doWithConnection(RServerConnection connection) {
            InputStream inputStream = armadilloStorage.loadTable(project, objectName);
            rExecutorService.loadTable(
                connection,
                new InputStreamResource(inputStream),
                table + PARQUET,
                symbol,
                variables);
            return null;
          }
        });
  }

  String readResource(InputStream is) throws IOException {
    GZIPInputStream gzis = new GZIPInputStream(is);
    InputStreamReader reader = new InputStreamReader(gzis);
    BufferedReader in = new BufferedReader(reader);
    String line;
    StringBuilder data = new StringBuilder();
    while ((line = in.readLine()) != null) {
      data.append(line);
    }
    return data.toString();
  }

  HashMap<String, String> extractResourceInfo(String fileInfo) {
    HashMap<String, String> resourceFileInfo = new HashMap<>();
    final String regex = "(\\/projects\\/)([\\w]+)(\\/objects\\/)(\\w+%2F[\\w_\\.]+)";
    final Matcher m = Pattern.compile(regex).matcher(fileInfo);
    while (m.find()) {
      resourceFileInfo.put("project", m.group(2));
      resourceFileInfo.put(
          "object", java.net.URLDecoder.decode(m.group(4), StandardCharsets.UTF_8));
    }
    return resourceFileInfo;
  }

  @Override
  public CompletableFuture<Void> loadResource(Principal principal, String symbol, String resource) {
    int index = resource.indexOf('/');
    String project = resource.substring(0, index);
    String objectName = resource.substring(index + 1);
    try {
      String resourceContent = readResource(armadilloStorage.loadResource(project, objectName));
      HashMap<String, String> resourceInfo = extractResourceInfo(resourceContent);
      String obj = resourceInfo.get("object");
      String resourceObjectName = armadilloStorage.getFilenameWithoutExtension(obj);
      JwtAuthenticationToken resourceAuth =
          resourceTokenService.generateResourceToken(
              principal, resourceInfo.get("project"), resourceObjectName);
      return schedule(
          new ArmadilloCommandImpl<>("Load resource " + resource, false) {
            @Override
            protected Void doWithConnection(RServerConnection connection) {
              InputStream inputStream = armadilloStorage.loadResource(project, objectName);
              rExecutorService.loadResource(
                  resourceAuth,
                  connection,
                  new InputStreamResource(inputStream),
                  resource + RDS,
                  symbol);
              return null;
            }
          });
    } catch (IOException e) {
      throw new StorageException("Cannot read contents of resource" + resource + "because: " + e);
    }
  }

  @Override
  public CompletableFuture<Void> saveWorkspace(Principal principal, String id) {
    return schedule(
        new ArmadilloCommandImpl<>("Save user workspace" + id, false) {
          @Override
          protected Void doWithConnection(RServerConnection connection) {
            rExecutorService.saveWorkspace(
                connection, is -> armadilloStorage.saveWorkspace(is, principal, id));
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<Void> installPackage(
      Principal principal, Resource resource, String name) {
    return schedule(
        new ArmadilloCommandImpl<>("Install package", false) {
          @Override
          protected Void doWithConnection(RServerConnection connection) {
            rExecutorService.installPackage(connection, resource, name);
            return null;
          }
        });
  }

  @Override
  public CompletableFuture<List<RPackage>> getPackages() {
    return schedule(
        new ArmadilloCommandImpl<>("getInstalledPackages", true) {
          @Override
          protected List<RPackage> doWithConnection(RServerConnection connection) {
            return packageService.getInstalledPackages(connection);
          }
        });
  }

  @PreDestroy
  public void preDestroy() {
    if (armadilloSession != null) armadilloSession.sessionCleanup();
  }
}
