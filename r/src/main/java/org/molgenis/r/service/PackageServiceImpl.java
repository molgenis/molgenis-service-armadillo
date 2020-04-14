package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.molgenis.r.Formatter.stringVector;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.r.REXPParser;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

/** Retrieves available {@link RPackage}s */
@Component
public class PackageServiceImpl implements PackageService {

  private final REXPParser rexpParser;

  public static final String FIELD_PACKAGE = "Package";
  public static final String FIELD_LIB_PATH = "LibPath";
  public static final String FIELD_VERSION = "Version";
  public static final String FIELD_BUILT = "Built";
  public static final String FIELD_AGGREGATE_METHODS = "AggregateMethods";
  public static final String FIELD_ASSIGN_METHODS = "AssignMethods";
  public static final String FIELD_OPTIONS = "Options";
  public static final String COMMAND_INSTALLED_PACKAGES =
      format(
          "installed.packages(fields=%s)",
          stringVector(FIELD_AGGREGATE_METHODS, FIELD_ASSIGN_METHODS, FIELD_OPTIONS));

  public PackageServiceImpl(REXPParser rexpParser) {
    this.rexpParser = rexpParser;
  }

  @Override
  public List<RPackage> getInstalledPackages(RConnection connection)
      throws RserveException, REXPMismatchException {
    REXPString matrix = (REXPString) connection.eval(COMMAND_INSTALLED_PACKAGES);
    List<Map<String, String>> rows = rexpParser.toStringMap(matrix);
    return rows.stream().map(PackageServiceImpl::toPackage).collect(Collectors.toList());
  }

  public static RPackage toPackage(Map<String, String> row) {
    RPackage.Builder builder =
        RPackage.builder()
            .setName(row.get(FIELD_PACKAGE))
            .setLibPath(row.get(FIELD_LIB_PATH))
            .setVersion(row.get(FIELD_VERSION))
            .setBuilt(row.get(FIELD_BUILT));
    if (row.containsKey(FIELD_OPTIONS)) {
      builder.setOptions(parseOptions(row.get(FIELD_OPTIONS)));
    }
    if (row.containsKey(FIELD_ASSIGN_METHODS)) {
      builder.setAssignMethods(parseMethods(row.get(FIELD_ASSIGN_METHODS)));
    }
    if (row.containsKey(FIELD_AGGREGATE_METHODS)) {
      builder.setAggregateMethods(parseMethods(row.get(FIELD_AGGREGATE_METHODS)));
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
