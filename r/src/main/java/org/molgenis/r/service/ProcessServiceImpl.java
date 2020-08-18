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
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
public class ProcessServiceImpl implements ProcessService {

  public static final String GET_RSERVE_PROCESSES_COMMAND =
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
  public static final String COUNT_RSERVE_PROCESSES_COMMAND =
      "library(magrittr)\n"
          + "ps::ps() %>%\n"
          + "  dplyr::filter(grepl(\"Rserve\", name)) %>%\n"
          + "  dplyr::count()";
  private final REXPParser rexpParser;

  public ProcessServiceImpl(REXPParser rexpParser) {
    this.rexpParser = rexpParser;
  }

  @Override
  public int countRserveProcesses(RConnection connection) {
    try {
      return ((REXP) connection.eval(COUNT_RSERVE_PROCESSES_COMMAND).asList().get(0)).asInteger();
    } catch (RserveException | REXPMismatchException | ClassCastException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public List<RProcess> getRserveProcesses(RConnection connection) {
    try {
      var result = connection.eval(GET_RSERVE_PROCESSES_COMMAND).asList();
      return rexpParser.parseTibble(result).stream().map(this::toRProcess).collect(toList());
    } catch (RserveException | REXPMismatchException e) {
      throw new RExecutionException(e);
    }
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
