package org.molgenis.datashield;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.datashield.DataShieldUtils.serializeCommand;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.molgenis.datashield.pojo.DataShieldCommand;
import org.molgenis.datashield.pojo.DataShieldCommandDTO;
import org.molgenis.datashield.service.DownloadService;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.Package;
import org.molgenis.r.model.Table;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.IdGenerator;
import org.springframework.web.bind.annotation.*;

@RestController
public class DataController {

  final DownloadService downloadService;
  final RExecutorService rExecutorService;
  final DataShieldSession datashieldSession;
  final PackageService packageService;
  final IdGenerator idGenerator;
  final StorageService storageService;

  public DataController(
      DownloadService downloadService,
      RExecutorService rExecutorService,
      DataShieldSession datashieldSession,
      PackageService packageService,
      StorageService storageService,
      IdGenerator idGenerator) {
    this.downloadService = downloadService;
    this.rExecutorService = rExecutorService;
    this.datashieldSession = datashieldSession;
    this.packageService = packageService;
    this.storageService = storageService;
    this.idGenerator = idGenerator;
  }

  @GetMapping("/load/{entityTypeId}/{assignSymbol}")
  @ResponseStatus(HttpStatus.OK)
  public void load(@PathVariable String entityTypeId, @PathVariable String assignSymbol) {
    Table table = downloadService.getMetadata(entityTypeId);
    ResponseEntity<Resource> response = downloadService.download(table);

    datashieldSession.execute(
        connection -> rExecutorService.assign(response.getBody(), assignSymbol, table, connection));
  }

  @GetMapping(value = "/lastresult", produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public CompletableFuture<ResponseEntity<byte[]>> lastResultRaw() {
    return Optional.ofNullable(datashieldSession.getLastExecution())
        .map(
            execution ->
                execution
                    .thenApply(DataShieldUtils::createRawResponse)
                    .thenApply(ResponseEntity::ok))
        .orElse(completedFuture(notFound().build()));
  }

  @GetMapping(value = "/lastresult", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public CompletableFuture<ResponseEntity<Object>> lastResultString() {
    return Optional.ofNullable(datashieldSession.getLastExecution())
        .map(
            execution ->
                execution
                    .thenApply(DataShieldUtils::asNativeJavaObject)
                    .thenApply(ResponseEntity::ok))
        .orElse(completedFuture(notFound().build()));
  }

  @PostMapping(value = "/execute", consumes = TEXT_PLAIN_VALUE, produces = APPLICATION_JSON_VALUE)
  public CompletableFuture<ResponseEntity<Object>> execute(
      @RequestBody String cmd, @RequestParam(defaultValue = "false") boolean async) {
    DataShieldCommand<REXP> command = datashieldSession.schedule(cmd);
    return async
        ? createdLastCommand()
        : command
            .getResult()
            .thenApply(DataShieldUtils::asNativeJavaObject)
            .thenApply(ResponseEntity::ok);
  }

  @PostMapping(
      value = "/execute",
      consumes = TEXT_PLAIN_VALUE,
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public CompletableFuture<ResponseEntity<byte[]>> executeRaw(
      @RequestBody String cmd, @RequestParam(defaultValue = "false") boolean async) {
    DataShieldCommand<REXP> command = datashieldSession.schedule(serializeCommand(cmd));
    return async
        ? createdLastCommand()
        : command
            .getResult()
            .thenApply(DataShieldUtils::createRawResponse)
            .thenApply(ResponseEntity::ok);
  }

  private static <R> CompletableFuture<ResponseEntity<R>> createdLastCommand() {
    return completedFuture(
        created(fromCurrentContextPath().replacePath("/lastcommand").build().toUri()).body(null));
  }

  /** @return command object (with expression and status) */
  @GetMapping(value = "/lastcommand", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<DataShieldCommandDTO> getLastCommand() {
    return Optional.ofNullable(datashieldSession.getLastCommand())
        .map(DataShieldCommandDTO::create)
        .map(ResponseEntity::ok)
        .orElse(notFound().build());
  }

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<Package> getPackages() {
    return datashieldSession.execute(packageService::getInstalledPackages);
  }

  /** @return a list of (fully qualified) table identifiers available for DataSHIELD operations. */
  @GetMapping(value = "/tables", produces = APPLICATION_JSON_VALUE)
  public List<String> getTables() {
    // TODO implement
    return Collections.emptyList();
  }

  /** @return true if the the table exists and is available for DataSHIELD operations. */
  @GetMapping("/exists/{entityTypeId}")
  public boolean exists(@PathVariable String entityTypeId) {
    return downloadService.metadataExists(entityTypeId);
  }

  /** @return a list of assigned symbols */
  @GetMapping(value = "/symbols", produces = APPLICATION_JSON_VALUE)
  public List<String> getSymbols() {
    // TODO implement
    return Collections.emptyList();
  }

  /** Assign the result of the evaluation of an expression to a symbol. */
  @PostMapping(
      value = "/symbols",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public CompletableFuture<Object> assignSymbol(
      @RequestBody String symbol, @RequestBody String expression) {
    // TODO implement
    return null;
  }

  /** Assign the result of the evaluation of an expression to a symbol. */
  @PostMapping(
      value = "/symbols",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public CompletableFuture<byte[]> assignSymbolRaw(
      @RequestBody String symbol, @RequestBody String expression) {
    // TODO implement
    return null;
  }

  /** Removes a symbol, making the assigned data inaccessible */
  @DeleteMapping(value = "/symbols/{symbol}")
  @ResponseStatus(HttpStatus.OK)
  public void removeSymbol(@PathVariable String symbol) {
    // TODO implement
  }

  /**
   * @return a list of available methods (with name, type ('aggregate' or 'assign'), class
   *     ('function' or 'script'), value, package, version.
   */
  @GetMapping(value = "/methods", produces = APPLICATION_JSON_VALUE)
  public List<String> getMethods() {
    // TODO implement
    return Collections.emptyList();
  }

  /** @return a list of workspaces (with lastAccessDate and size) */
  @GetMapping(value = "/workspaces", produces = APPLICATION_JSON_VALUE)
  public List<String> getWorkspaces() {
    // TODO implement
    return Collections.emptyList();
  }

  /**
   * Deletes a workspace (fails silently if the workspace doesn't exist)
   *
   * @param id the id of the saved workspace
   */
  @DeleteMapping(value = "/workspaces/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void removeWorkspace(@PathVariable String id) {
    // TODO implement
  }

  @PostMapping(value = "/save-workspace", produces = TEXT_PLAIN_VALUE)
  public String save() {
    UUID saveId = idGenerator.generateId();
    String objectname = String.format("%s/.RData", saveId.toString());
    datashieldSession.execute(
        connection -> {
          rExecutorService.saveWorkspace(
              connection, is -> storageService.save(is, objectname, APPLICATION_OCTET_STREAM));
          return null;
        });
    return saveId.toString();
  }

  @PostMapping(value = "/load-workspace/{saveId}")
  public void loadWorkspace(@PathVariable String saveId) {
    UUID uuid = UUID.fromString(saveId);
    String objectname = String.format("%s/.RData", uuid.toString());
    datashieldSession.execute(
        connection -> {
          InputStream inputStream = storageService.load(objectname);
          rExecutorService.loadWorkspace(connection, new InputStreamResource(inputStream));
          return null;
        });
  }

  /**
   * @return info object (with repo.version and repo.name, the db.name, username, host, port, etc.)
   */
  @GetMapping(value = "/info", produces = APPLICATION_JSON_VALUE)
  public String getInfo() {
    // TODO implement
    return null;
  }
}
