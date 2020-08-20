package org.molgenis.r.service;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.molgenis.r.REXPParser;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.model.RProcess;
import org.molgenis.r.model.RProcess.Status;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcessServiceImpl implements ProcessService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessServiceImpl.class);
  static final String GET_RSERVE_PROCESSES_COMMAND =
      "library(magrittr)\n"
          + "getPorts <- function(handle) {\n"
          + "  tryCatch(ps::ps_connections(handle) %>%\n"
          + "    dplyr::filter(is.integer(lport) & !is.na(lport)) %>%\n"
          + "    dplyr::pull(lport) %>%\n"
          + "    paste0(collapse = \" \"), error = function(e) {NA})\n"
          + "}\n"
          + "\n"
          + "getCmd <- function(handle) {\n"
          + "  tryCatch(ps::ps_cmdline(handle) %>%\n"
          + "             paste0(collapse = \" \"), error = function(e) {NA})\n"
          + "}\n"
          + "\n"
          + "ps::ps() %>%\n"
          + "  dplyr::filter(grepl(\"Rserve\", name)) %>%\n"
          + "  "
          + "dplyr::mutate(\n"
          + "    ports = sapply(ps_handle, getPorts, simplify = \"vector\"),\n"
          + "    cmd = sapply(ps_handle, getCmd, simplify = \"vector\")\n"
          + "  "
          + ") ";
  static final String COUNT_RSERVE_PROCESSES_COMMAND =
      "library(magrittr)\n"
          + "ps::ps() %>%\n"
          + "  dplyr::filter(grepl(\"Rserve\", name)) %>%\n"
          + "  dplyr::count()";
  static final String GET_PID_COMMAND = "ps::ps_pid(ps::ps_handle())";
  static final String TERMINATE_COMMAND = "ps::ps_terminate(ps::ps_handle(%dL))";
  private final REXPParser rexpParser;
  private final RExecutorService rExecutorService;

  public ProcessServiceImpl(REXPParser rexpParser, RExecutorService rExecutorService) {
    this.rexpParser = rexpParser;
    this.rExecutorService = rExecutorService;
  }

  @Override
  public int countRserveProcesses(RConnection connection) {
    try {
      return ((REXP)
              rExecutorService.execute(COUNT_RSERVE_PROCESSES_COMMAND, connection).asList().get(0))
          .asInteger();
    } catch (REXPMismatchException | ClassCastException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public List<RProcess> getRserveProcesses(RConnection connection) {
    try {
      var result = rExecutorService.execute(GET_RSERVE_PROCESSES_COMMAND, connection).asList();
      return rexpParser.parseTibble(result).stream().map(this::toRProcess).collect(toList());
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public int getPid(RConnection connection) {
    try {
      return rExecutorService.execute(GET_PID_COMMAND, connection).asInteger();
    } catch (REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void terminateProcess(RConnection connection, int pid) {
    LOGGER.info("Terminating R Process with pid {}", pid);
    rExecutorService.execute(String.format(TERMINATE_COMMAND, pid), connection);
  }

  private RProcess toRProcess(Map<String, Object> values) {
    var builder = RProcess.builder();
    Optional.ofNullable((Integer) values.get("pid")).ifPresent(builder::setPid);
    Optional.ofNullable((Integer) values.get("ppid")).ifPresent(builder::setPPid);
    Optional.ofNullable((String) values.get("name")).ifPresent(builder::setName);
    Optional.ofNullable((String) values.get("cmd")).ifPresent(builder::setCmd);
    Optional.ofNullable((String) values.get("username")).ifPresent(builder::setUsername);
    Optional.ofNullable((String) values.get("status"))
        .map(String::toUpperCase)
        .map(Status::valueOf)
        .ifPresent(builder::setStatus);
    Optional.ofNullable((Double) values.get("created"))
        .flatMap(rexpParser::parseDate)
        .ifPresent(builder::setCreated);
    Optional.ofNullable((String) values.get("ports"))
        .filter(it -> !it.isEmpty())
        .map(it -> it.split(" "))
        .map(it -> Arrays.stream(it).map(Integer::parseInt).collect(toList()))
        .ifPresent(builder::setPorts);
    Optional.ofNullable((Double) values.get("user")).ifPresent(builder::setUser);
    Optional.ofNullable((Double) values.get("system")).ifPresent(builder::setSystem);
    Optional.ofNullable((Double) values.get("rss")).ifPresent(builder::setRss);
    Optional.ofNullable((Double) values.get("vms")).ifPresent(builder::setVms);
    return builder.build();
  }
}
