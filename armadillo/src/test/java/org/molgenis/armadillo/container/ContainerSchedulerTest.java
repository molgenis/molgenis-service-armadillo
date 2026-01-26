package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.*;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContainerSchedulerTest {

  @Mock private DockerService dockerService;
  @Mock private ContainerService containerService;
  @Mock private DockerClient dockerClient;
  @Mock private ScheduledFuture<?> scheduledFuture;

  @InjectMocks private ContainerScheduler containerScheduler;

  @Test
  void testToCronWeekly() throws Exception {
    var schedule = new UpdateSchedule("weekly", "Monday", "10:30");
    var cron = invokeToCron(schedule);
    assertEquals("0 30 10 * * 1", cron);
  }

  @Test
  void testToCronDailyFallback() throws Exception {
    var schedule = new UpdateSchedule("daily", null, "08:15");
    var cron = invokeToCron(schedule);
    assertEquals("0 15 08 * * *", cron);
  }

  @Test
  void testToCronWithNullValuesUsesDefaults() throws Exception {
    var schedule = new UpdateSchedule(null, null, null); // all null
    var cron = invokeToCron(schedule);
    // Defaults: time = "01:00", frequency = "weekly", day = "Sunday" (mapped to 0)
    assertEquals("0 00 01 * * 0", cron);
  }

  @Test
  void testDayToCronNumber() throws Exception {
    assertEquals(0, invokeDayToCronNumber("Sunday"));
    assertEquals(1, invokeDayToCronNumber("Monday"));
    assertEquals(2, invokeDayToCronNumber("Tuesday"));
    assertEquals(3, invokeDayToCronNumber("Wednesday"));
    assertEquals(4, invokeDayToCronNumber("Thursday"));
    assertEquals(5, invokeDayToCronNumber("Friday"));
    assertEquals(6, invokeDayToCronNumber("Saturday"));
  }

  @Test
  void testDayToCronNumberDefaultCase() throws Exception {
    assertEquals(0, invokeDayToCronNumber("NotADay")); // Covers 'default -> 0'
  }

  @Test
  void testRescheduleCreatesTaskWithCronTrigger() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getUpdateSchedule()).thenReturn(new UpdateSchedule("daily", null, "09:00"));

    var scheduler = containerScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(containerScheduler, "taskScheduler", spyScheduler);

    doReturn(scheduledFuture)
        .when(spyScheduler)
        .schedule(any(Runnable.class), any(CronTrigger.class));

    containerScheduler.reschedule(container);

    verify(spyScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void reschedule_runsScheduledTask() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getUpdateSchedule()).thenReturn(new UpdateSchedule("daily", null, "09:00"));

    when(dockerService.getAllContainerStatuses()).thenReturn(Map.of());

    var scheduler = containerScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(containerScheduler, "taskScheduler", spyScheduler);

    var runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    doReturn(scheduledFuture)
        .when(spyScheduler)
        .schedule(runnableCaptor.capture(), any(CronTrigger.class));

    containerScheduler.reschedule(container);
    runnableCaptor.getValue().run();

    verify(dockerService).getAllContainerStatuses();
  }

  @Test
  void testScheduleWithDateOverload() {
    var scheduler = containerScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(containerScheduler, "taskScheduler", spyScheduler);

    Date startTime = new Date();

    doReturn(scheduledFuture).when(spyScheduler).schedule(any(Runnable.class), any(Date.class));

    spyScheduler.schedule(() -> {}, startTime);

    verify(spyScheduler).schedule(any(Runnable.class), any(Date.class));
  }

  @Test
  void testCancelRemovesTask() {
    var scheduler = containerScheduler.taskScheduler();
    ReflectionTestUtils.setField(containerScheduler, "taskScheduler", scheduler);

    var tasksMap =
        (Map<String, ScheduledFuture<?>>)
            ReflectionTestUtils.getField(containerScheduler, "scheduledTasks");
    tasksMap.put("testContainer", scheduledFuture);

    containerScheduler.cancel("testContainer");

    verify(scheduledFuture).cancel(false);
  }

  @Mock private ContainerInfo containerInfo; // Add this mock

  @Test
  void testRunUpdateForContainerImageChanged() throws Exception {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getImage()).thenReturn("timmyjc/mytest:latest");
    when(container.getLastImageId()).thenReturn("oldImage");

    // Mock container status (RUNNING so update check proceeds)
    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    var containerStatusMap = Map.of("testContainer", containerInfo);
    when(dockerService.getAllContainerStatuses()).thenReturn(containerStatusMap);

    // Mock dockerClient.pullImageCmd
    var pullImageCmd = mock(PullImageCmd.class);
    var pullImageResult = mock(PullImageResultCallback.class);
    when(dockerClient.pullImageCmd("timmyjc/mytest:latest")).thenReturn(pullImageCmd);
    when(pullImageCmd.start()).thenReturn(pullImageResult);
    when(pullImageResult.awaitCompletion()).thenReturn(null);

    // Mock dockerClient.inspectImageCmd
    var inspectImageCmd = mock(InspectImageCmd.class);
    var inspectImageResponse = mock(InspectImageResponse.class);
    when(dockerClient.inspectImageCmd("timmyjc/mytest:latest")).thenReturn(inspectImageCmd);
    when(inspectImageCmd.exec()).thenReturn(inspectImageResponse);
    when(inspectImageResponse.getId()).thenReturn("newImage");

    // Mock hasImageIdChanged -> true so restart triggers
    when(dockerService.hasImageIdChanged("testContainer", "oldImage", "newImage")).thenReturn(true);

    // Execute
    invokeRunUpdateForContainer(container);

    verify(dockerService).pullImageStartContainer("testContainer");
  }

  @Test
  void testRunUpdateForContainerNoInfo() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer"); // ✅ Still needed
    when(dockerService.getAllContainerStatuses()).thenReturn(Map.of()); // No container info
    invokeRunUpdateForContainer(container);
    verify(dockerService, never()).pullImageStartContainer(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
  }

  @Test
  void testRunUpdateForContainerAutoUpdateDisabled() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(false); // ✅ Needed for branch exit

    // Only stub container info and its status
    var containerInfo = mock(ContainerInfo.class);
    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    when(dockerService.getAllContainerStatuses())
        .thenReturn(Map.of("testContainer", containerInfo));

    invokeRunUpdateForContainer(container);

    verify(dockerService, never()).pullImageStartContainer(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
    verify(containerService, never()).getByName(any());
  }

  @Test
  void testRunUpdateForContainerImageUnchanged() throws Exception {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getImage()).thenReturn("timmyjc/mytest:latest");
    when(container.getLastImageId()).thenReturn("oldImage");

    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    when(dockerService.getAllContainerStatuses())
        .thenReturn(Map.of("testContainer", containerInfo));

    // Mock pull and inspect
    var pullImageCmd = mock(PullImageCmd.class);
    var pullImageResult = mock(PullImageResultCallback.class);
    when(dockerClient.pullImageCmd("timmyjc/mytest:latest")).thenReturn(pullImageCmd);
    when(pullImageCmd.start()).thenReturn(pullImageResult);
    when(pullImageResult.awaitCompletion()).thenReturn(null);

    var inspectImageCmd = mock(InspectImageCmd.class);
    var inspectImageResponse = mock(InspectImageResponse.class);
    when(dockerClient.inspectImageCmd("timmyjc/mytest:latest")).thenReturn(inspectImageCmd);
    when(inspectImageCmd.exec()).thenReturn(inspectImageResponse);
    when(inspectImageResponse.getId()).thenReturn("oldImage"); // Same ID -> unchanged

    when(dockerService.hasImageIdChanged("testContainer", "oldImage", "oldImage"))
        .thenReturn(false);

    invokeRunUpdateForContainer(container);

    // Verify NO restart
    verify(dockerService, never()).pullImageStartContainer(any());
  }

  @Test
  void testRunUpdateForContainerHandlesException() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer"); // needed for logging

    // Throw exception when fetching container info (caught inside try/catch)
    when(dockerService.getAllContainerStatuses()).thenThrow(new RuntimeException("Test exception"));

    invokeRunUpdateForContainer(container); // Should log error but not rethrow

    // Verify: nothing else was called
    verify(containerService, never()).getByName(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
  }

  @Test
  void testRunUpdateForContainerSkipsWhenImageNull() {
    ContainerConfig container =
        (ContainerConfig)
            mock(UpdatableContainer.class, withSettings().extraInterfaces(ContainerConfig.class));
    when(container.getName()).thenReturn("testContainer");
    when(container.getImage()).thenReturn(null);
    when(((UpdatableContainer) container).getAutoUpdate()).thenReturn(true);

    when(dockerService.getAllContainerStatuses())
        .thenReturn(Map.of("testContainer", ContainerInfo.create(ContainerStatus.RUNNING)));

    invokeRunUpdateForContainer(container);

    verify(dockerClient, never()).pullImageCmd(anyString());
    verify(dockerService, never()).pullImageStartContainer(anyString());
  }

  @Test
  void testRunUpdateForContainerSkipsWhenImageEmpty() {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getImage()).thenReturn("");

    when(dockerService.getAllContainerStatuses())
        .thenReturn(Map.of("testContainer", ContainerInfo.create(ContainerStatus.RUNNING)));

    invokeRunUpdateForContainer(container);

    verify(dockerClient, never()).pullImageCmd(anyString());
    verify(dockerService, never()).pullImageStartContainer(anyString());
  }

  @Test
  void testRunUpdateForContainerHandlesInterruptedException() throws Exception {
    var container = mock(DatashieldContainerConfig.class);
    when(container.getName()).thenReturn("testContainer");
    when(container.getAutoUpdate()).thenReturn(true);
    when(container.getImage()).thenReturn("timmyjc/mytest:latest");

    when(dockerService.getAllContainerStatuses())
        .thenReturn(Map.of("testContainer", ContainerInfo.create(ContainerStatus.RUNNING)));

    var pullImageCmd = mock(PullImageCmd.class);
    var pullImageResult = mock(PullImageResultCallback.class);
    when(dockerClient.pullImageCmd("timmyjc/mytest:latest")).thenReturn(pullImageCmd);
    when(pullImageCmd.start()).thenReturn(pullImageResult);
    when(pullImageResult.awaitCompletion()).thenThrow(new InterruptedException("boom"));

    invokeRunUpdateForContainer(container);

    assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag should be set");
    Thread.interrupted(); // clear for other tests
  }

  // --- Helper methods to invoke private methods ---
  private String invokeToCron(UpdateSchedule schedule) throws Exception {
    var method = ContainerScheduler.class.getDeclaredMethod("toCron", UpdateSchedule.class);
    method.setAccessible(true);
    return (String) method.invoke(containerScheduler, schedule);
  }

  private int invokeDayToCronNumber(String day) throws Exception {
    var method = ContainerScheduler.class.getDeclaredMethod("convertDayToCronNumber", String.class);
    method.setAccessible(true);
    return (int) method.invoke(containerScheduler, day);
  }

  private void invokeRunUpdateForContainer(ContainerConfig container) {
    try {
      var method =
          ContainerScheduler.class.getDeclaredMethod(
              "runUpdateForContainer", ContainerConfig.class); // Changed to the interface
      method.setAccessible(true);
      method.invoke(containerScheduler, container);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
