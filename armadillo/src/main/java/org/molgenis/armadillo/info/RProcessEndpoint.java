package org.molgenis.armadillo.info;

import java.util.List;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RProcess;
import org.molgenis.r.service.ProcessServiceImpl;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "r-processes")
public class RProcessEndpoint {
  private final ProcessServiceImpl processService;
  private final RConnectionFactory rConnectionFactory;

  public RProcessEndpoint(
      ProcessServiceImpl processService, RConnectionFactory rConnectionFactory) {
    this.processService = processService;
    this.rConnectionFactory = rConnectionFactory;
  }

  @ReadOperation
  public List<RProcess> getRProcesses() {
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      return processService.getProcesses(connection);
    } finally {
      connection.close();
    }
  }
}
