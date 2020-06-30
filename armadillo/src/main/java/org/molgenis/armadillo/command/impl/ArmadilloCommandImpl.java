package org.molgenis.armadillo.command.impl;

import static java.time.Clock.systemUTC;
import static org.molgenis.armadillo.command.ArmadilloCommandDTO.builder;
import static org.molgenis.armadillo.command.Commands.ArmadilloCommandStatus.*;

import com.google.common.base.Throwables;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.molgenis.armadillo.command.ArmadilloCommand;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.ArmadilloCommandDTO.Builder;
import org.molgenis.armadillo.command.Commands.ArmadilloCommandStatus;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.MDC;

public abstract class ArmadilloCommandImpl<T> implements ArmadilloCommand<T> {
  private final Instant createDate;
  private final UUID id;
  private final Map<String, String> contextMap;
  protected final String expression;
  private final boolean withResult;
  private final Clock clock;

  @SuppressWarnings("java:S3077") // Optional is immutable and CompletableFuture is thread-safe
  private volatile CompletableFuture<T> execution;

  private volatile Instant startDate;
  private volatile Instant endDate;

  ArmadilloCommandImpl(String expression, boolean withResult) {
    this(expression, withResult, systemUTC());
  }

  // For test purposes, allow the clock to be mocked
  ArmadilloCommandImpl(String expression, boolean withResult, Clock clock) {
    this.contextMap = MDC.getCopyOfContextMap();
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
    if (contextMap != null) {
      contextMap.forEach(MDC::put);
    }
    this.startDate = clock.instant();
  }

  synchronized void complete() {
    MDC.clear();
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

  public synchronized ArmadilloCommandStatus getStatus() {
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
  public synchronized ArmadilloCommandDTO asDto() {
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
