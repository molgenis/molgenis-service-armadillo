package org.molgenis.datashield.pojo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.COMPLETED;
import static org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus.PENDING;
import static org.molgenis.datashield.pojo.DataShieldCommandDTO.builder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataShieldCommandDTOTest {

  @Mock DataShieldCommand<Integer> command;
  Instant createDate = Instant.now();
  Instant startDate = createDate.plusMillis(1000);
  Instant endDate = startDate.plusMillis(2000);
  UUID id = UUID.randomUUID();

  @Test
  void createPending() {
    when(command.getCreateDate()).thenReturn(createDate);
    when(command.getExpression()).thenReturn("Life, the universe, and everything");
    when(command.getStatus()).thenReturn(PENDING);
    when(command.getId()).thenReturn(id);
    when(command.isWithResult()).thenReturn(true);

    DataShieldCommandDTO actual = DataShieldCommandDTO.create(command);

    DataShieldCommandDTO expected =
        builder()
            .createDate(createDate)
            .expression("Life, the universe, and everything")
            .status(PENDING)
            .id(id)
            .withResult(true)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void createCompleted() {
    when(command.getCreateDate()).thenReturn(createDate);
    when(command.getExpression()).thenReturn("Life, the universe, and everything");
    when(command.getStartDate()).thenReturn(Optional.of(startDate));
    when(command.getEndDate()).thenReturn(Optional.of(endDate));
    when(command.getStatus()).thenReturn(COMPLETED);
    when(command.getId()).thenReturn(id);
    when(command.isWithResult()).thenReturn(true);

    DataShieldCommandDTO actual = DataShieldCommandDTO.create(command);

    DataShieldCommandDTO expected =
        builder()
            .createDate(createDate)
            .startDate(startDate)
            .endDate(endDate)
            .expression("Life, the universe, and everything")
            .endDate(endDate)
            .status(COMPLETED)
            .id(id)
            .withResult(true)
            .build();
    assertEquals(expected, actual);
  }
}
