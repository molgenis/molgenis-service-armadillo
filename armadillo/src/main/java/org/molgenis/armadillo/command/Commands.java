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

  List<Workspace> listWorkspaces(String prefix);

  CompletableFuture<List<String>> loadWorkspaces(List<String> objectNames);

  CompletableFuture<Void> loadUserWorkspace(String objectName);

  CompletableFuture<Void> saveWorkspace(String objectName);

  void removeWorkspace(String objectName);

  CompletableFuture<List<RPackage>> getPackages();

  Optional<CompletableFuture<REXP>> getLastExecution();

  Optional<DataShieldCommandDTO> getLastCommand();

  enum DataShieldCommandStatus {
    COMPLETED,
    FAILED,
    PENDING,
    IN_PROGRESS
  }
}
