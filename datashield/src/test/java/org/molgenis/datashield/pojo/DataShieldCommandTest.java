package org.molgenis.datashield.pojo;

import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.COMPLETED;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.FAILED;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.IN_PROGRESS;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.PENDING;
import static org.molgenis.datashield.pojo.DataShieldCommandDTO.builder;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataShieldCommandTest {

  @Mock private Clock clock;
  private DataShieldCommand command;
  private Instant createDate = now();

  @BeforeEach
  void setUp() {
    when(clock.instant()).thenReturn(createDate);
    command = new DataShieldCommand("expression", true, clock);
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
    command.setResult(new CompletableFuture());

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
    command.setResult(new CompletableFuture());
    command.start();

    assertEquals(IN_PROGRESS, command.getStatus());
  }

  @Test
  void failed() {
    command.setResult(failedFuture(new RuntimeException("Failed")));

    assertEquals(FAILED, command.getStatus());
    assertEquals(Optional.of("Failed"), command.getMessage());
  }

  @Test
  void completed() {
    command.setResult(completedFuture(42));

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
    command.setResult(completedFuture(42));
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
    command.setResult(failedFuture(new RuntimeException("Failed")));
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
