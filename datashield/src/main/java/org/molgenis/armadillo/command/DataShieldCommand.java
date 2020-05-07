package org.molgenis.armadillo.command;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DataShieldCommand<T> {

  Optional<CompletableFuture<T>> getExecution();

  DataShieldCommandDTO asDto();
}
