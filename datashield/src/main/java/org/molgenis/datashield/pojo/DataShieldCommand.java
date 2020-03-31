package org.molgenis.datashield.pojo;

import static java.time.Instant.now;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataShieldCommand<T> {
  private final Instant createDate;
  private final UUID id;
  private final String expression;
  private final boolean withResult;
  @SuppressWarnings("java:S3077") // CompletableFuture is thread-safe
  private volatile CompletableFuture<T> result;
  @SuppressWarnings("java:S3077") // Optional is immutable
  private volatile Optional<Instant> startDate = Optional.empty();
  @SuppressWarnings("java:S3077") // Optional is immutable
  private volatile Optional<Instant> endDate = Optional.empty();

  public enum DataShieldCommandStatus {
    COMPLETED,
    FAILED,
    PENDING,
    IN_PROGRESS
  }

  public DataShieldCommand(String expression) {
    this.expression = expression;
    this.withResult = true;
    this.createDate = now();
    this.id = UUID.randomUUID();
  }

  public void setResult(CompletableFuture<T> result) {
    this.result = result;
  }

  public void start() {
    this.startDate = Optional.of(now());
  }

  public void complete() {
    this.endDate = Optional.of(now());
  }

  public CompletableFuture<T> getResult() {
    return this.result;
  }

  public Optional<Instant> getStartDate() {
    return startDate;
  }

  public Instant getCreateDate() {
    return createDate;
  }

  public Optional<Instant> getEndDate() {
    return endDate;
  }

  public UUID getId() {
    return id;
  }

  public String getExpression() {
    return expression;
  }

  public boolean isWithResult() {
    return withResult;
  }

  public DataShieldCommandStatus getStatus() {
    if (startDate.isEmpty()) {
      return PENDING;
    }
    if (result.isCompletedExceptionally() || result.isCancelled()) {
      return FAILED;
    }
    if (result.isDone()) {
      return COMPLETED;
    }
    return IN_PROGRESS;
  }
}
