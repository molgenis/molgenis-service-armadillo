package org.molgenis.datashield.pojo;

import static java.time.Clock.systemUTC;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataShieldCommand<T> {
  private final Instant createDate;
  private final UUID id;
  private final String expression;
  private final boolean withResult;
  private final Clock clock;

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
    this(expression, systemUTC());
  }

  // For test purposes, allow the clock to be mocked
  DataShieldCommand(String expression, Clock clock) {
    this.expression = expression;
    this.withResult = true;
    this.createDate = clock.instant();
    this.id = UUID.randomUUID();
    this.clock = clock;
  }

  public void setResult(CompletableFuture<T> result) {
    this.result = result;
  }

  public void start() {
    this.startDate = Optional.of(clock.instant());
  }

  public void complete() {
    this.endDate = Optional.of(clock.instant());
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
    if (result == null) {
      return PENDING;
    }
    if (result.isCompletedExceptionally() || result.isCancelled()) {
      return FAILED;
    }
    if (result.isDone()) {
      return COMPLETED;
    }
    if (startDate.isEmpty()) {
      return PENDING;
    }
    return IN_PROGRESS;
  }
}
