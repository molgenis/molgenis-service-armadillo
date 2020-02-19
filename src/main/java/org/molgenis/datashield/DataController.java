package org.molgenis.datashield;

import java.io.IOException;
import java.io.InputStream;
import org.molgenis.datashield.exceptions.DatashieldRequestFailedException;
import org.molgenis.datashield.r.RDatashieldSession;
import org.molgenis.datashield.service.DownloadServiceImpl;
import org.molgenis.datashield.service.RExecutorServiceImpl;
import org.molgenis.datashield.service.model.Table;
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

  final DownloadServiceImpl downloadService;
  final RExecutorServiceImpl executorService;
  final RDatashieldSession datashieldSession;

  public DataController(
      DownloadServiceImpl downloadService,
      RExecutorServiceImpl executorService,
      RDatashieldSession datashieldSession) {
    this.downloadService = downloadService;
    this.executorService = executorService;
    this.datashieldSession = datashieldSession;
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
            throw new DatashieldRequestFailedException(err.getMessage(), err);
          }
        });
  }

  @PostMapping(
      value = "/execute",
      consumes = MediaType.TEXT_PLAIN_VALUE,
      produces = MediaType.TEXT_PLAIN_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public String execute(@RequestBody String cmd) throws REXPMismatchException, RserveException {
    return datashieldSession.execute(connection -> executorService.execute(cmd, connection));
  }
}
