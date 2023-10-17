package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.r.service.ProcessServiceImpl.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.RNamedList;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.model.RProcess;
import org.molgenis.r.model.RProcess.Status;
import org.molgenis.r.rock.RockResult;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.REXPMismatchException;

@ExtendWith(MockitoExtension.class)
class ProcessServiceImplTest {
  @Mock private RExecutorService rExecutorService;
  @Mock private RServerResult rexp;
  @Mock private RNamedList<RServerResult> list;
  @Mock private RServerConnection rConnection;

  private ProcessService processService;

  @BeforeEach
  void before() {
    processService = new ProcessServiceImpl(rExecutorService);
  }

  @Test
  void testGetPid() throws REXPMismatchException {
    when(rExecutorService.execute(GET_PID_COMMAND, rConnection)).thenReturn(rexp);
    when(rexp.asInteger()).thenReturn(218);

    assertEquals(218, processService.getPid(rConnection));
  }

  @Test
  void testTerminate() throws RServerException {
    processService.terminateProcess(rConnection, 218);

    verify(rExecutorService).execute(format(TERMINATE_COMMAND, 218), rConnection);
  }

  @Test
  void testCountRserveProcesses() throws REXPMismatchException, RServerException {
    when(rExecutorService.execute(COUNT_RSERVE_PROCESSES_COMMAND, rConnection))
        .thenReturn(new RockResult(new REXPList(new REXPInteger(3), "n")));
    assertEquals(3, processService.countRserveProcesses(rConnection));
  }

  @Test
  void testGetRserveProcesses() {
    when(rExecutorService.execute(GET_RSERVE_PROCESSES_COMMAND, rConnection)).thenReturn(rexp);
    when(rexp.asNamedList()).thenReturn(list);
    when(list.asRows())
        .thenReturn(
            List.of(
                Map.ofEntries(
                    entry("pid", 645),
                    entry("ppid", 632),
                    entry("name", "vpnkit-bridge"),
                    entry("username", "root"),
                    entry("status", "running"),
                    entry("user", 3.475336003),
                    entry("system", 3.907483292),
                    entry("rss", 10137600.0),
                    entry("vms", 4513603584.0),
                    entry("created", 1597214141.41565),
                    entry("cmd", "vpnkit-bridge --addr listen://1999 host"),
                    entry("ports", "1234 5678")),
                Map.ofEntries(
                    entry("pid", 531),
                    entry("ppid", 1),
                    entry("name", "SafeEjectGPUAgent"),
                    entry("username", "root"),
                    entry("status", "running"),
                    entry("user", 0.007282068),
                    entry("system", 0.012664273),
                    entry("rss", 3411968.0),
                    entry("vms", 4938244096.0),
                    entry("created", 1597214127.64387),
                    entry("cmd", "SafeEjectGPUAgent"),
                    entry("ports", ""))));

    assertEquals(
        List.of(
            RProcess.builder()
                .setPid(645)
                .setPPid(632)
                .setName("vpnkit-bridge")
                .setCmd("vpnkit-bridge --addr listen://1999 host")
                .setUsername("root")
                .setStatus(Status.RUNNING)
                .setUser(3.475336003)
                .setSystem(3.907483292)
                .setRss(10137600.0)
                .setVms(4513603584.0)
                .setCreated(Instant.parse("2020-08-12T06:35:41.416Z"))
                .setCmd("vpnkit-bridge --addr listen://1999 host")
                .setPorts(List.of(1234, 5678))
                .build(),
            RProcess.builder()
                .setPid(531)
                .setPPid(1)
                .setName("SafeEjectGPUAgent")
                .setCmd("SafeEjectGPUAgent")
                .setUsername("root")
                .setStatus(Status.RUNNING)
                .setUser(0.007282068)
                .setSystem(0.012664273)
                .setRss(3411968.0)
                .setVms(4938244096.0)
                .setCreated(Instant.parse("2020-08-12T06:35:27.644Z"))
                .setCmd("SafeEjectGPUAgent")
                .build()),
        processService.getRserveProcesses(rConnection));
  }
}
