package org.molgenis.r.service;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.molgenis.r.Formatter;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.FailedRPackageInstallException;
import org.molgenis.r.exceptions.InvalidRPackageException;
import org.molgenis.r.exceptions.RExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class RExecutorServiceImpl implements RExecutorService {

//  KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//    kpg.initialize(2048);
//  keyPair = kpg.generateKeyPair();

  private static final Logger LOGGER = LoggerFactory.getLogger(RExecutorServiceImpl.class);

  @Override
  public RServerResult execute(String cmd, boolean serialized, RServerConnection connection) {
    try {
      LOGGER.debug("Evaluate {}", cmd);
      RServerResult result = connection.eval(cmd, serialized);
      if (result == null) {
        throw new RExecutionException("Eval returned null");
      }
      return result;
    } catch (RServerException e) {
      LOGGER.warn("RServerException", e);
      throw new RExecutionException(e);
    }
  }

  @Override
  public void saveWorkspace(
      RServerConnection connection, Consumer<InputStream> inputStreamConsumer) {
    try {
      LOGGER.debug("Save workspace");
      String command = "base::save.image()";
      execute(command, connection);
      connection.readFile(".RData", inputStreamConsumer);
    } catch (RServerException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadWorkspace(RServerConnection connection, Resource resource, String environment) {
    LOGGER.debug("Load workspace into {}", environment);
    try {
      copyFile(resource, ".RData", connection);
      connection.eval(format("base::load(file='.RData', envir=%s)", environment));
      connection.eval("base::unlink('.RData')");
    } catch (IOException | RServerException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadTable(
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol,
      List<String> variables) {
    LOGGER.debug("Load table from file {} into {}", filename, symbol);
    String rFileName = filename.replace("/", "_");
    try {
      copyFile(resource, rFileName, connection);
      if (variables.isEmpty()) {
        execute(
            format(
                "is.null(base::assign('%s', value={data.frame(arrow::read_parquet('%s', as_data_frame = FALSE))}))",
                symbol, rFileName),
            connection);
      } else {
        String colSelect =
            "tidyselect::any_of("
                + Formatter.stringVector(variables.toArray(new String[] {}))
                + ")";
        execute(
            format(
                "is.null(base::assign('%s', value={data.frame(arrow::read_parquet('%s', as_data_frame = FALSE, col_select = %s))}))",
                symbol, rFileName, colSelect),
            connection);
      }
      execute(format("base::unlink('%s')", rFileName), connection);
    } catch (IOException | RServerException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadResource(
      Principal principal,
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol) {
    LOGGER.debug("Load resource from file {} into {}", filename, symbol);
    String rFileName = filename.replace("/", "_");
    try {
      if (principal instanceof JwtAuthenticationToken token) {

        //later create a system token here
//        Jwt newJwt = Jwt.withTokenValue("new")
//                .header("alg", "RS256")
//                .claim("email", o)
//                .claim("extra", "super intern geheim")
//                .issuedAt(Instant.now())
//                .expiresAt(Instant.now().plusSeconds(300))
//                .build();

        String tokenValue = token.getToken().getTokenValue();
        copyFile(resource, rFileName, connection);
        execute(format("is.null(base::assign('rds',base::readRDS('%s')))", rFileName), connection);
        execute(format("base::unlink('%s')", rFileName), connection);
        execute(
            format(
                """
                                  is.null(base::assign('R', value={resourcer::newResource(
                                          name = rds$name,
                                          url = gsub(rds$url, '/objects/', '/rawfile/',
                                          format = rds$format
                                          secret = "%s"
                                  )}))""",
                tokenValue),
            connection);
      }
      execute(
          format("is.null(base::assign('%s', value={resourcer::newResourceClient(R)}))", symbol),
          connection);
    } catch (Exception e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void installPackage(
      RServerConnection connection, Resource packageResource, String filename) {
    // https://stackoverflow.com/questions/30989027/how-to-install-a-package-from-a-download-zip-file

    if (!filename.endsWith(".tar.gz")) {
      throw new InvalidRPackageException(filename);
    }

    String packageName = getPackageNameFromFilename(filename);

    LOGGER.info("Installing package '{}'", filename);
    String rFilename = getRFilenameFromFilename(filename);
    try {
      copyFile(packageResource, rFilename, connection);
      execute(
          format("remotes::install_local('%s', dependencies = TRUE, upgrade = 'never')", rFilename),
          connection);
      RServerResult result = execute(format("require('%s')", packageName), connection);
      if (!result.asLogical()) {
        throw new FailedRPackageInstallException(packageName);
      }
      execute(format("file.remove('%s')", filename), connection);

    } catch (IOException | RServerException e) {
      throw new RExecutionException(e);
    }
  }

  void copyFile(Resource resource, String dataFileName, RServerConnection connection)
      throws IOException, RServerException {
    LOGGER.info("Copying '{}' to R...", dataFileName);
    try (InputStream is = resource.getInputStream()) {
      connection.writeFile(dataFileName, is);
    }
  }

  protected String getPackageNameFromFilename(String filename) {
    return filename.replaceFirst("_[^_]+$", "");
  }

  protected String getRFilenameFromFilename(String filename) {
    return filename.replace("/", "_");
  }
}
