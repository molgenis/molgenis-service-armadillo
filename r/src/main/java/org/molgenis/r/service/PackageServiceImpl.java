package org.molgenis.r.service;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.ObjectUtils.isEmpty;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.model.RPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Retrieves available {@link RPackage}s */
@Component
public class PackageServiceImpl implements PackageService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PackageServiceImpl.class);

  public static final String FIELD_PACKAGE = "Package";
  public static final String FIELD_LIB_PATH = "LibPath";
  public static final String FIELD_VERSION = "Version";
  public static final String FIELD_BUILT = "Built";
  public static final String FIELD_AGGREGATE_METHODS = "AggregateMethods";
  public static final String FIELD_ASSIGN_METHODS = "AssignMethods";
  public static final String FIELD_OPTIONS = "Options";
  public static final String COMMAND_INSTALLED_PACKAGES =
      "library(\"magrittr\")\n"
          + "\n"
          + "to_df <- function(lst) {\n"
          + "  tibble::as_tibble(lapply(lst, function(x) t(tibble::as_tibble(x))))\n"
          + "}\n"
          + "\n"
          + "read_datashield_inst <- function(package) {\n"
          + "  file <- system.file(\"DATASHIELD\", package = package$Package)\n"
          + "  if (file == \"\") package\n"
          + "  else {\n"
          + "    from_file <- as.list(unlist(tibble::as_tibble(read.dcf(file))))\n"
          + "    result <- append(as.list(package), as.list(from_file))\n"
          + "    result[!is.na(result)]\n"
          + "  }\n"
          + "}\n"
          + "\n"
          + "installed.packages(fields = c('AssignMethods', 'AggregateMethods', 'Options')) %>%\n"
          + "  tibble::as_tibble() %>%\n"
          + "  dplyr::rowwise() %>%\n"
          + "  dplyr::do(to_df(read_datashield_inst(.)))\n";

  @Override
  public List<RPackage> getInstalledPackages(RServerConnection connection) {
    try {
      RServerResult eval = connection.eval(COMMAND_INSTALLED_PACKAGES);
      List<Map<String, Object>> rows = eval.asNamedList().asRows();
      return rows.stream().map(PackageServiceImpl::toPackage).collect(Collectors.toList());
    } catch (RServerException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadPackages(RServerConnection connection, Set<String> pkgs) {
    String packages = String.format("\"%s\"", String.join("\",\"", pkgs));
    try {
      LOGGER.trace("Loading packages [ {} ]", packages);
      connection.eval("base::lapply(c(" + packages + "), library, character.only = TRUE)");
      LOGGER.trace("Successfully loaded packages [ {} ]", packages);
    } catch (RServerException e) {
      throw new RExecutionException("Error loading packages: " + packages);
    }
  }

  public static RPackage toPackage(Map<String, Object> row) {
    RPackage.Builder builder =
        RPackage.builder()
            .setName((String) row.get(FIELD_PACKAGE))
            .setLibPath((String) row.get(FIELD_LIB_PATH))
            .setVersion((String) row.get(FIELD_VERSION))
            .setBuilt((String) row.get(FIELD_BUILT));
    if (!isEmpty(row.get(FIELD_OPTIONS))) {
      builder.setOptions(parseOptions((String) row.get(FIELD_OPTIONS)));
    }
    if (!isEmpty(row.get(FIELD_ASSIGN_METHODS))) {
      builder.setAssignMethods(parseMethods((String) row.get(FIELD_ASSIGN_METHODS)));
    }
    if (!isEmpty(row.get(FIELD_AGGREGATE_METHODS))) {
      builder.setAggregateMethods(parseMethods((String) row.get(FIELD_AGGREGATE_METHODS)));
    }
    return builder.build();
  }

  // TODO: check out DataShieldROptionsParser in opal, values can contain commas?
  static ImmutableMap<String, String> parseOptions(String options) {
    Map<String, String> optionsMap =
        stream(options.split(","))
            .map(it -> it.split("="))
            .collect(toMap(it -> it[0].trim(), it -> it[1].trim()));
    return ImmutableMap.copyOf(optionsMap);
  }

  static ImmutableSet<String> parseMethods(String aggregateMethods) {
    String[] methods = aggregateMethods.split(",");
    Set<String> methodSet = stream(methods).map(String::trim).collect(Collectors.toSet());
    return ImmutableSet.copyOf(methodSet);
  }
}
