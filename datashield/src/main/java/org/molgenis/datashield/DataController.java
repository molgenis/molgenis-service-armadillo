package org.molgenis.datashield;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.datashield.DataShieldUtils.serializeCommand;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.notFound;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
  @ResponseStatus(HttpStatus.OK)
  public CompletableFuture<Object> execute(@RequestBody String cmd) {
    CompletableFuture<REXP> result =
        datashieldSession.schedule(connection -> rExecutorService.execute(cmd, connection));
    return result.thenApply(DataShieldUtils::asNativeJavaObject);
  }

  @PostMapping(
      value = "/execute",
      consumes = TEXT_PLAIN_VALUE,
      produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public CompletableFuture<byte[]> executeRaw(@RequestBody String cmd) {
    CompletableFuture<REXP> result =
        datashieldSession.schedule(
            connection -> rExecutorService.execute(serializeCommand(cmd), connection));
    return result.thenApply(DataShieldUtils::createRawResponse);
  }

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<Package> getPackages() {
    return datashieldSession.execute(packageService::getInstalledPackages);
  }

  @GetMapping("/exists/{entityTypeId}")
  public boolean exists(@PathVariable String entityTypeId) {
    return downloadService.metadataExists(entityTypeId);
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
}
