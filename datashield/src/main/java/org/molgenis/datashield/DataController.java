package org.molgenis.datashield;

import org.molgenis.datashield.service.DownloadService;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.Package;
import org.molgenis.r.model.Table;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.IdGenerator;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static org.molgenis.datashield.DataShieldUtils.createRawResponse;
import static org.molgenis.datashield.DataShieldUtils.serializeCommand;
import static org.springframework.http.MediaType.*;

@RestController
public class DataController {

  final DownloadService downloadService;
  final RExecutorService executorService;
  final DataShieldSession datashieldSession;
  final PackageService packageService;
  final IdGenerator idGenerator;
  final StorageService storageService;

  public DataController(
      DownloadService downloadService,
      RExecutorService executorService,
      DataShieldSession datashieldSession,
      PackageService packageService,
      StorageService storageService,
      IdGenerator idGenerator) {
    this.downloadService = downloadService;
    this.executorService = executorService;
    this.datashieldSession = datashieldSession;
    this.packageService = packageService;
    this.storageService = storageService;
    this.idGenerator = idGenerator;
  }

  @GetMapping("/load/{entityTypeId}")
  @ResponseStatus(HttpStatus.OK)
  public void load(@PathVariable String entityTypeId)
      throws REXPMismatchException, RserveException {
    Table table = downloadService.getMetadata(entityTypeId);
    ResponseEntity<Resource> response = downloadService.download(table);

    datashieldSession.execute(
        connection -> executorService.assign(response.getBody(), table, connection));
  }

  @PostMapping(
      value = "/execute",
      consumes = TEXT_PLAIN_VALUE,
      produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String execute(@RequestBody String cmd) throws REXPMismatchException, RserveException {
    REXP result = datashieldSession.execute(connection -> executorService.execute(cmd, connection));
    return result.isNull() ? "null" : result.asString();
  }

  @PostMapping(
          value = "/execute/raw",
          consumes = TEXT_PLAIN_VALUE,
          produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public byte[] executeRaw(@RequestBody String cmd) throws RserveException, REXPMismatchException
  {
    REXP result = datashieldSession.execute(connection -> executorService.execute(serializeCommand(cmd), connection));
    return createRawResponse(result);
  }

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<Package> getPackages() throws REXPMismatchException, RserveException {
    return datashieldSession.execute(packageService::getInstalledPackages);
  }

  @PostMapping(value = "/save-workspace", produces = TEXT_PLAIN_VALUE)
  public String save() throws REXPMismatchException, RserveException {
    UUID saveId = idGenerator.generateId();
    String objectname = String.format("%s/.RData", saveId.toString());
    datashieldSession.execute(
        connection -> {
          executorService.saveWorkspace(
              connection, is -> storageService.save(is, objectname, APPLICATION_OCTET_STREAM));
          return null;
        });
    return saveId.toString();
  }

  @PostMapping(value = "/load-workspace/{saveId}")
  public void loadWorkspace(@PathVariable String saveId)
      throws REXPMismatchException, RserveException {
    UUID uuid = UUID.fromString(saveId);
    String objectname = String.format("%s/.RData", uuid.toString());
    datashieldSession.execute(
        connection -> {
          InputStream inputStream = storageService.load(objectname);
          executorService.loadWorkspace(connection, new InputStreamResource(inputStream));
          return null;
        });
  }
}
