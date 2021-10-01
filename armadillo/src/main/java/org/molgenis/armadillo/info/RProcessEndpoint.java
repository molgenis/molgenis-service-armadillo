package org.molgenis.armadillo.info;

import static org.molgenis.r.RServers.DEFAULT;

import java.util.List;
import org.molgenis.armadillo.ArmadilloSessionFactory;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServers;
import org.molgenis.r.model.RProcess;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "rserveProcesses")
public class RProcessEndpoint {
  private final ProcessService processService;
  private final RServers rServers;

  public RProcessEndpoint(ProcessService processService, RServers rServers) {
    this.processService = processService;
    this.rServers = rServers;
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
