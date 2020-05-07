package org.molgenis.armadillo.command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ArmadilloCommand<T> {

  Optional<CompletableFuture<T>> getExecution();

  ArmadilloCommandDTO asDto();
}
