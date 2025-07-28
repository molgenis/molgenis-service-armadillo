package org.molgenis.armadillo.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.AutoUpdateSchedule;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfileStatus;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileSchedulerTest {

  @Mock private DockerService dockerService;
  @Mock private ProfileService profileService;
  @Mock private DockerClient dockerClient;
  @Mock private ScheduledFuture<?> scheduledFuture;

  @InjectMocks private ProfileScheduler profileScheduler;

  @Test
  void testToCronWeekly() throws Exception {
    var schedule = new AutoUpdateSchedule("weekly", "Monday", "10:30");
    var cron = invokeToCron(schedule);
    assertEquals("0 30 10 * * 1", cron);
  }

  @Test
  void testToCronDailyFallback() throws Exception {
    var schedule = new AutoUpdateSchedule("daily", null, "08:15");
    var cron = invokeToCron(schedule);
    assertEquals("0 15 08 * * *", cron);
  }

  @Test
  void testDayToCronNumber() throws Exception {
    assertEquals(0, invokeDayToCronNumber("Sunday"));
    assertEquals(3, invokeDayToCronNumber("Wednesday"));
    assertEquals(6, invokeDayToCronNumber("Saturday"));
  }

  @Test
  void testRescheduleCreatesTaskWithCronTrigger() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(true);
    when(profile.getAutoUpdateSchedule())
        .thenReturn(new AutoUpdateSchedule("daily", null, "09:00"));

    var scheduler = profileScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(profileScheduler, "taskScheduler", spyScheduler);

    doReturn(scheduledFuture)
        .when(spyScheduler)
        .schedule(any(Runnable.class), any(CronTrigger.class));

    profileScheduler.reschedule(profile);

    verify(spyScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void testScheduleWithDateOverload() {
    var scheduler = profileScheduler.taskScheduler();
    var spyScheduler = spy(scheduler);
    ReflectionTestUtils.setField(profileScheduler, "taskScheduler", spyScheduler);

    Date startTime = new Date();

    doReturn(scheduledFuture).when(spyScheduler).schedule(any(Runnable.class), any(Date.class));

    spyScheduler.schedule(() -> {}, startTime);

    verify(spyScheduler).schedule(any(Runnable.class), any(Date.class));
  }

  @Test
  void testCancelRemovesTask() {
    var scheduler = profileScheduler.taskScheduler();
    ReflectionTestUtils.setField(profileScheduler, "taskScheduler", scheduler);

    var tasksMap =
        (Map<String, ScheduledFuture<?>>)
            ReflectionTestUtils.getField(profileScheduler, "scheduledTasks");
    tasksMap.put("testProfile", scheduledFuture);

    profileScheduler.cancel("testProfile");

    verify(scheduledFuture).cancel(false);
  }

  @Mock private ContainerInfo containerInfo; // Add this mock

  @Test
  void testRunUpdateForProfileImageChanged() {
    var profile = mock(ProfileConfig.class);
    when(profile.getName()).thenReturn("testProfile");
    when(profile.getAutoUpdate()).thenReturn(true);

    // Mock ContainerInfo to return RUNNING status
    when(containerInfo.getStatus()).thenReturn(ProfileStatus.RUNNING);
    var profileStatusMap = Map.of("testProfile", containerInfo);
    when(dockerService.getAllProfileStatuses()).thenReturn(profileStatusMap);

    when(profileService.getByName("testProfile")).thenReturn(profile);
    when(profile.getLastImageId()).thenReturn("oldImage");

    var inspectCmd = mock(InspectContainerCmd.class);
    var inspectResponse = mock(InspectContainerResponse.class);
    when(dockerClient.inspectContainerCmd(any())).thenReturn(inspectCmd);
    when(inspectCmd.exec()).thenReturn(inspectResponse);
    when(inspectResponse.getImageId()).thenReturn("newImage");

    when(dockerService.asContainerName("testProfile")).thenReturn("containerName");
    when(dockerService.hasImageIdChanged("oldImage", "newImage")).thenReturn(true);

    invokeRunUpdateForProfile(profile);

    verify(dockerService).startProfile("testProfile");
  }

  // --- Helper methods to invoke private methods ---
  private String invokeToCron(AutoUpdateSchedule schedule) throws Exception {
    var method = ProfileScheduler.class.getDeclaredMethod("toCron", AutoUpdateSchedule.class);
    method.setAccessible(true);
    return (String) method.invoke(profileScheduler, schedule);
  }

  private int invokeDayToCronNumber(String day) throws Exception {
    var method = ProfileScheduler.class.getDeclaredMethod("dayToCronNumber", String.class);
    method.setAccessible(true);
    return (int) method.invoke(profileScheduler, day);
  }

  private void invokeRunUpdateForProfile(ProfileConfig profile) {
    try {
      var method =
          ProfileScheduler.class.getDeclaredMethod("runUpdateForProfile", ProfileConfig.class);
      method.setAccessible(true);
      method.invoke(profileScheduler, profile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
