package org.molgenis.armadillo.command;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXP;

public interface Commands {

  CompletableFuture<REXP> evaluate(String expression);

  CompletableFuture<Void> assign(String symbol, String expression);

  List<String> listSharedTables();

  CompletableFuture<Void> loadTable(String symbol, String table, String variables);

  List<Workspace> listWorkspaces(String bucketName);

  CompletableFuture<Void> loadUserWorkspace(String bucketName, String objectName);

  CompletableFuture<Void> saveWorkspace(String bucketName, String objectName);

  void removeWorkspace(String bucketName, String objectName);

  CompletableFuture<List<RPackage>> getPackages();

  Optional<CompletableFuture<REXP>> getLastExecution();

  Optional<ArmadilloCommandDTO> getLastCommand();

  enum ArmadilloCommandStatus {
    COMPLETED,
    FAILED,
    PENDING,
    IN_PROGRESS
  }
}
