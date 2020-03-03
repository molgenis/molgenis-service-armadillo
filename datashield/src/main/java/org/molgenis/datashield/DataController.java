package org.molgenis.datashield;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.molgenis.datashield.exceptions.DataShieldRequestFailedException;
import org.molgenis.datashield.service.DownloadService;
import org.molgenis.r.model.Package;
import org.molgenis.r.model.Table;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataController {

  final DownloadService downloadService;
  final RExecutorService executorService;
  final DataShieldSession datashieldSession;
  final PackageService packageService;

  public DataController(
      DownloadService downloadService,
      RExecutorService executorService,
      DataShieldSession datashieldSession,
      PackageService packageService) {
    this.downloadService = downloadService;
    this.executorService = executorService;
    this.datashieldSession = datashieldSession;
    this.packageService = packageService;
  }

  @GetMapping("/load/{entityTypeId}")
  @ResponseStatus(HttpStatus.OK)
  public void load(@PathVariable String entityTypeId)
      throws REXPMismatchException, RserveException {
    Table table = downloadService.getMetadata(entityTypeId);
    ResponseEntity<Resource> response = downloadService.download(table);

    datashieldSession.execute(
        connection -> {
          try {
            InputStream csvStream = response.getBody().getInputStream();
            return executorService.assign(csvStream, table, connection);
          } catch (IOException err) {
            throw new DataShieldRequestFailedException(err.getMessage(), err);
          }
        });
  }

  @PostMapping(
      value = "/execute",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String execute(@RequestBody String cmd) throws REXPMismatchException, RserveException {
    REXP result = datashieldSession.execute(connection -> executorService.execute(cmd, connection));
    return result.isNull() ? "null" : result.asString();
  }

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<Package> getPackages() throws REXPMismatchException, RserveException {
    return datashieldSession.execute(packageService::getInstalledPackages);
  }
}
