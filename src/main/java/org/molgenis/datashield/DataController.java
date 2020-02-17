package org.molgenis.datashield;

import org.molgenis.datashield.r.RDatashieldSession;
import org.molgenis.datashield.service.DownloadServiceImpl;
import org.molgenis.datashield.service.RExecutorServiceImpl;
import org.molgenis.datashield.service.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class DataController
{

  @Autowired DownloadServiceImpl downloadService;

  @Autowired RExecutorServiceImpl executorService;

  @GetMapping("/load/{entityTypeId}")
  @ResponseStatus(HttpStatus.OK)
  public void load(@PathVariable String entityTypeId, RDatashieldSession datashieldSession)
          throws REXPMismatchException, RserveException, IOException
  {
    Table table = downloadService.getMetadata(entityTypeId);
    ResponseEntity<Resource> response = downloadService.download(table);



    datashieldSession.execute(connection -> {
      try {
      InputStream csvStream = response.getBody().getInputStream();
      return executorService.assign(csvStream, table, connection);
      } catch(IOException err) {
        throw new RuntimeException(err.getMessage(), err);
      }
    });


  }



}
