package org.molgenis.armadillo.command.impl;

import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.command.Commands.DataShieldCommandStatus.COMPLETED;
import static org.molgenis.armadillo.command.Commands.DataShieldCommandStatus.FAILED;
import static org.molgenis.armadillo.command.Commands.DataShieldCommandStatus.IN_PROGRESS;
import static org.molgenis.armadillo.command.Commands.DataShieldCommandStatus.PENDING;
import static org.molgenis.armadillo.command.DataShieldCommandDTO.builder;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.command.DataShieldCommandDTO;
import org.rosuda.REngine.Rserve.RConnection;

@ExtendWith(MockitoExtension.class)
public class DataShieldCommandImplTest {

  @Mock private Clock clock;
  private DataShieldCommandImpl command;
  private Instant createDate = now();

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(createDate);
    command =
        new DataShieldCommandImpl("expression", true, clock) {

          @Override
          protected RConnection doWithConnection(RConnection connection) {
            return connection;
          }
        };
  }

  @Test
  void createDate() {
    assertEquals(createDate, command.getCreateDate());
  }

  @Test
  void expression() {
    assertEquals("expression", command.getExpression());
  }

  @Test
  void message() {
    assertEquals(Optional.empty(), command.getMessage());
  }

  @Test
  void id() {
    assertNotNull(command.getId());
  }

  @Test
  void isWithResult() {
    assertTrue(command.isWithResult());
  }

  @Test
  void statusPendingNoResult() {
    assertEquals(PENDING, command.getStatus());
  }

  @Test
  void statusPendingNotStarted() {
    command.setExecution(new CompletableFuture());

    assertEquals(PENDING, command.getStatus());
  }

  @Test
  void start() {
    Instant startDate = now();
    when(clock.instant()).thenReturn(startDate);

    command.start();

    assertEquals(Optional.of(startDate), command.getStartDate());
  }

  @Test
  void complete() {
    Instant endDate = now();
    when(clock.instant()).thenReturn(endDate);

    command.complete();

    assertEquals(Optional.of(endDate), command.getEndDate());
  }

  @Test
  void inProgress() {
    command.setExecution(new CompletableFuture());
    command.start();

    assertEquals(IN_PROGRESS, command.getStatus());
  }

  @Test
  void failed() {
    command.setExecution(failedFuture(new RuntimeException("Failed")));

    assertEquals(FAILED, command.getStatus());
    assertEquals(Optional.of("Failed"), command.getMessage());
  }

  @Test
  void completed() {
    command.setExecution(completedFuture(42));

    assertEquals(COMPLETED, command.getStatus());
  }

  @Test
  void asDtoPending() {
    DataShieldCommandDTO actual = command.asDto();

    DataShieldCommandDTO expected =
        builder()
            .createDate(command.getCreateDate())
            .expression(command.getExpression())
            .status(PENDING)
            .id(command.getId())
            .withResult(true)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void asDtoCompleted() {
    command.start();
    command.setExecution(completedFuture(42));
    command.complete();

    DataShieldCommandDTO actual = command.asDto();

    DataShieldCommandDTO expected =
        builder()
            .createDate(command.getCreateDate())
            .startDate(((Optional<Instant>) command.getStartDate()).get())
            .endDate(((Optional<Instant>) command.getEndDate()).get())
            .expression(command.getExpression())
            .status(COMPLETED)
            .id(command.getId())
            .withResult(true)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void asDtoFailed() {
    command.start();
    command.setExecution(failedFuture(new RuntimeException("Failed")));
    command.complete();

    DataShieldCommandDTO actual = command.asDto();

    DataShieldCommandDTO expected =
        builder()
            .createDate(command.getCreateDate())
            .startDate(((Optional<Instant>) command.getStartDate()).get())
            .endDate(((Optional<Instant>) command.getEndDate()).get())
            .expression(command.getExpression())
            .status(FAILED)
            .message("Failed")
            .id(command.getId())
            .withResult(true)
            .build();
    assertEquals(expected, actual);
  }
}
