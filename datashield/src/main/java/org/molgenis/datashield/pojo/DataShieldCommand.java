package org.molgenis.datashield.pojo;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataShieldCommand<T> {

  private CompletableFuture<T> result;
  private Instant startDate;
  private Instant createDate;
  private Instant endDate;
  private UUID id;
  private String expression;
  private boolean withResult;
  private DataShieldCommandStatus status;

  public DataShieldCommand(CompletableFuture<T> result, String expression) {
    this.result = result;
    this.expression = expression;
    this.withResult = true;
    this.createDate = Instant.now();
    this.id = UUID.randomUUID();
    this.status = DataShieldCommandStatus.PENDING;

    result.thenAccept(this::finish).exceptionally(this::failed);
  }

  public void start() {
    this.startDate = Instant.now();
    this.status = DataShieldCommandStatus.IN_PROGRESS;
  }

  private void finish(T result) {
    this.endDate = Instant.now();
    this.status = DataShieldCommandStatus.COMPLETED;
  }

  private Void failed(Throwable failure) {
    this.status = DataShieldCommandStatus.FAILED;

    return null;
  }

  public CompletableFuture<T> getResult() {
    return this.result;
  }
}
