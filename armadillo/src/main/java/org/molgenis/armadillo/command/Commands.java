package org.molgenis.armadillo.command;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXP;
import org.springframework.core.io.Resource;

public interface Commands {

  List<String> listProfiles();

  String getActiveProfileName();

  void selectProfile(String profileName);

  CompletableFuture<REXP> evaluate(String expression);

  CompletableFuture<Void> assign(String symbol, String expression);

  CompletableFuture<Void> loadTable(
      String symbol, String project, String table, List<String> variables);

  CompletableFuture<Void> loadResource(String symbol, String project, String resource);

  CompletableFuture<Void> loadWorkspace(Principal principal, String id);

  CompletableFuture<Void> saveWorkspace(Principal principal, String id);

  CompletableFuture<Void> installPackage(Principal principal, Resource resource, String name);

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
