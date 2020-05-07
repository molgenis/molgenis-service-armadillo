package org.molgenis.armadillo.command.impl;

import static java.time.Clock.systemUTC;
import static org.molgenis.armadillo.command.Commands.DataShieldCommandStatus.*;
import static org.molgenis.armadillo.command.DataShieldCommandDTO.builder;

import com.google.common.base.Throwables;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.command.Commands.DataShieldCommandStatus;
import org.molgenis.armadillo.command.DataShieldCommand;
import org.molgenis.armadillo.command.DataShieldCommandDTO;
import org.molgenis.armadillo.command.DataShieldCommandDTO.Builder;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.Rserve.RConnection;

public abstract class DataShieldCommandImpl<T> implements DataShieldCommand<T> {
  private final Instant createDate;
  private final UUID id;
  protected final String expression;
  private final boolean withResult;
  private final Clock clock;

  @SuppressWarnings("java:S3077") // Optional is immutable and CompletableFuture is thread-safe
  private volatile CompletableFuture<T> execution;

  private volatile Instant startDate;
  private volatile Instant endDate;

  DataShieldCommandImpl(String expression, boolean withResult) {
    this(expression, withResult, systemUTC());
  }

  // For test purposes, allow the clock to be mocked
  DataShieldCommandImpl(String expression, boolean withResult, Clock clock) {
    this.expression = expression;
    this.withResult = withResult;
    this.createDate = clock.instant();
    this.id = UUID.randomUUID();
    this.clock = clock;
  }

  public synchronized void setExecution(CompletableFuture<T> execution) {
    this.execution = execution;
  }

  synchronized void start() {
    this.startDate = clock.instant();
  }

  synchronized void complete() {
    this.endDate = clock.instant();
  }

  @Override
  public synchronized Optional<CompletableFuture<T>> getExecution() {
    return Optional.ofNullable(execution);
  }

  public Optional<Instant> getStartDate() {
    return Optional.ofNullable(startDate);
  }

  public Optional<String> getMessage() {
    if (getStatus() == FAILED) {
      try {
        execution.get();
      } catch (Exception e) {
        return Optional.of(e).map(Throwables::getRootCause).map(Throwable::getMessage);
      }
    }
    return Optional.empty();
  }

  public Instant getCreateDate() {
    return createDate;
  }

  public Optional<Instant> getEndDate() {
    return Optional.ofNullable(endDate);
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

  public synchronized DataShieldCommandStatus getStatus() {
    if (execution == null) {
      return PENDING;
    }
    if (execution.isCompletedExceptionally() || execution.isCancelled()) {
      return FAILED;
    }
    if (execution.isDone()) {
      return COMPLETED;
    }
    if (startDate == null) {
      return PENDING;
    }
    return IN_PROGRESS;
  }

  public T evaluate(RConnection connection) {
    start();
    try {
      return doWithConnection(connection);
    } catch (Exception ex) {
      throw new RExecutionException(ex);
    } finally {
      complete();
    }
  }

  protected abstract T doWithConnection(RConnection connection);

  @Override
  public synchronized DataShieldCommandDTO asDto() {
    Builder builder =
        builder()
            .createDate(getCreateDate())
            .expression(getExpression())
            .id(getId())
            .status(getStatus())
            .withResult(isWithResult());
    getMessage().ifPresent(builder::message);
    getStartDate().ifPresent(builder::startDate);
    getEndDate().ifPresent(builder::endDate);
    return builder.build();
  }
}
