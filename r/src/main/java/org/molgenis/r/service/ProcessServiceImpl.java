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
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
public class ProcessServiceImpl implements ProcessService {

  public static final String GET_PROCESSES_COMMAND =
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
          + "dplyr::mutate(\n"
          + "  ps::ps(),\n"
          + "  ports = sapply(ps_handle, getPorts, simplify = \"vector\"),\n"
          + "  cmd = sapply(ps_handle, getCmd, simplify = \"vector\")\n"
          + ")";
  private final REXPParser rexpParser;

  public ProcessServiceImpl(REXPParser rexpParser) {
    this.rexpParser = rexpParser;
  }

  @Override
  public List<RProcess> getProcesses(RConnection connection) {
    try {
      var result = connection.eval(GET_PROCESSES_COMMAND).asList();
      return rexpParser.parseTibble(result).stream().map(this::toRProcess).collect(toList());
    } catch (RserveException | REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  private RProcess toRProcess(Map<String, Object> values) {
    var ports =
        Optional.ofNullable((String) values.get("ports"))
            .filter(it -> !it.isEmpty())
            .map(it -> it.split(" "))
            .map(it -> Arrays.stream(it).map(Integer::parseInt).collect(toList()))
            .orElse(List.of());
    var builder =
        RProcess.builder()
            .setPid((Integer) values.get("pid"))
            .setPPid((Integer) values.get("ppid"))
            .setName((String) values.get("name"))
            .setCmd((String) values.get("cmd"))
            .setUsername((String) values.get("username"))
            .setStatus(Status.valueOf(((String) values.get("status")).toUpperCase()))
            .setCreated(rexpParser.parseDate((Double) values.get("created")))
            .setPorts(ports);
    Optional.ofNullable((Double) values.get("user")).ifPresent(builder::setUser);
    Optional.ofNullable((Double) values.get("system")).ifPresent(builder::setSystem);
    Optional.ofNullable((Double) values.get("rss")).ifPresent(builder::setRss);
    Optional.ofNullable((Double) values.get("vms")).ifPresent(builder::setVms);
    return builder.build();
  }
}
