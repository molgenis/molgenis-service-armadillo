package org.molgenis.armadillo.command;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.profile.Profile;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXP;

public interface Commands {

  Profile getCurrentProfile();

  List<String> listProfiles();

  Optional<Profile> selectProfile(String profileName);

  CompletableFuture<REXP> evaluate(String expression);

  CompletableFuture<Void> assign(String symbol, String expression);

  CompletableFuture<Void> loadTable(String symbol, String table, List<String> variables);

  CompletableFuture<Void> loadResource(String symbol, String resource);

  CompletableFuture<Void> loadWorkspace(Principal principal, String id);

  CompletableFuture<Void> saveWorkspace(Principal principal, String id);

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
