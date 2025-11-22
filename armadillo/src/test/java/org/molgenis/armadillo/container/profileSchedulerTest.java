package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(true);
    when(profile.getUpdateSchedule()).thenReturn(new UpdateSchedule("daily", null, "09:00"));

    var scheduler = containerScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(containerScheduler, "taskScheduler", spyScheduler);

    doReturn(scheduledFuture)
        .when(spyScheduler)
        .schedule(any(Runnable.class), any(CronTrigger.class));

    containerScheduler.reschedule(profile);

    verify(spyScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
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
    tasksMap.put("testProfile", scheduledFuture);

    containerScheduler.cancel("testProfile");

    verify(scheduledFuture).cancel(false);
  }

  @Mock private ContainerInfo containerInfo; // Add this mock

  @Test
  void testRunUpdateForProfileImageChanged() throws Exception {
    // Mock container
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(true);
    when(profile.getImage()).thenReturn("timmyjc/mytest:latest");
    when(profile.getLastImageId()).thenReturn("oldImage");

    // Mock container status (RUNNING so update check proceeds)
    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    var profileStatusMap = Map.of("testProfile", containerInfo);
    when(dockerService.getAllProfileStatuses()).thenReturn(profileStatusMap);

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
    when(dockerService.hasImageIdChanged("testProfile", "oldImage", "newImage")).thenReturn(true);

    // Execute
    invokeRunUpdateForProfile(profile);

    // Verify startProfile is invoked
    verify(dockerService).startProfile("testProfile");
  }

  @Test
  void testRunUpdateForProfileNoContainerInfo() {
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile"); // ✅ Still needed
    when(dockerService.getAllProfileStatuses()).thenReturn(Map.of()); // No container info
    invokeRunUpdateForProfile(profile);
    verify(dockerService, never()).startProfile(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
  }

  @Test
  void testRunUpdateForProfileAutoUpdateDisabled() {
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(false); // ✅ Needed for branch exit

    // Only stub container info and its status
    var containerInfo = mock(ContainerInfo.class);
    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    when(dockerService.getAllProfileStatuses()).thenReturn(Map.of("testProfile", containerInfo));

    invokeRunUpdateForProfile(profile);

    verify(dockerService, never()).startProfile(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
    verify(containerService, never()).getByName(any());
  }

  @Test
  void testRunUpdateForProfileImageUnchanged() throws Exception {
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(true);
    when(profile.getImage()).thenReturn("timmyjc/mytest:latest");
    when(profile.getLastImageId()).thenReturn("oldImage");

    when(containerInfo.getStatus()).thenReturn(ContainerStatus.RUNNING);
    when(dockerService.getAllProfileStatuses()).thenReturn(Map.of("testProfile", containerInfo));

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

    when(dockerService.hasImageIdChanged("testProfile", "oldImage", "oldImage")).thenReturn(false);

    invokeRunUpdateForProfile(profile);

    // Verify NO restart
    verify(dockerService, never()).startProfile(any());
  }

  @Test
  void testRunUpdateForProfileHandlesException() {
    var profile = mock(ContainerConfig.class);
    when(profile.getName()).thenReturn("testProfile"); // needed for logging

    // Throw exception when fetching container info (caught inside try/catch)
    when(dockerService.getAllProfileStatuses()).thenThrow(new RuntimeException("Test exception"));

    invokeRunUpdateForProfile(profile); // Should log error but not rethrow

    // Verify: nothing else was called
    verify(containerService, never()).getByName(any());
    verify(dockerClient, never()).inspectContainerCmd(any());
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

  private void invokeRunUpdateForProfile(ContainerConfig profile) {
    try {
      var method =
          ContainerScheduler.class.getDeclaredMethod("runUpdateForProfile", ContainerConfig.class);
      method.setAccessible(true);
      method.invoke(containerScheduler, profile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
