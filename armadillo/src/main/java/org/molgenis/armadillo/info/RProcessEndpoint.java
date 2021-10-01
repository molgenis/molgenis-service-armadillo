package org.molgenis.armadillo.info;


import org.molgenis.r.RServeEnvironments;
import org.molgenis.r.service.ProcessService;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "rserveProcesses")
public class RProcessEndpoint {
  private final ProcessService processService;
  private final RServeEnvironments rServeEnvironments;

  public RProcessEndpoint(ProcessService processService, RServeEnvironments rServeEnvironments) {
    this.processService = processService;
    this.rServeEnvironments = rServeEnvironments;
  }
//  TODO: make this somehow RESTful?
//  public List<String> getProfiles() {
//
//  }
//
//  @ReadOperation
//  public List<RProcess> getRServeProcesses(String profileName) {
//    var rConnectionFactory = rServers.getConnectionFactory(DEFAULT);
//    final RConnection connection = rConnectionFactory.createConnection();
//    try {
//      return processService.getRserveProcesses(connection);
//    } finally {
//      connection.close();
//    }
//  }
//
//  @DeleteOperation
//  public void deleteRServeProcess(String profileName, int pid) {
//    final RConnection connection = rConnectionFactory.createConnection();
//    try {
//      processService.terminateProcess(connection, pid);
//    } finally {
//      connection.close();
//    }
//  }
//
//  public int countRServeProcesses() {
//    final RConnection connection = rConnectionFactory.createConnection();
//    try {
//      return processService.countRserveProcesses(connection);
//    } finally {
//      connection.close();
//    }
//  }
}
