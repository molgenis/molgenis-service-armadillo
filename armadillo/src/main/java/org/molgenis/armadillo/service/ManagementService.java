package org.molgenis.armadillo.service;

import static java.lang.String.format;

import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.molgenis.armadillo.config.ConfigFile;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.storage.JarDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ManagementService {

  private static final String ARMADILLO_JAR = "molgenis-armadillo-%s.jar";

  @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}")
  String armadilloHome;

  @Value("${armadillo.armadillo-mode:PROD}")
  String armadilloMode;

  @Value("${armadillo.docker-run-in-container:false}")
  private boolean runningInContainer;

  BuildProperties buildProperties;
  String armadilloConfigFile;

  private final RebootScriptRunner scriptRunner;
  private final ConfigFile appConfigFile;
  private final String jarHome;
  private final JarDownloader jarDownloader;
  private final GithubApi githubApi;
  private final UpdateScriptDownloader updateScriptDownloader;
  private final OidcDetails currentOidcDetails;

  @Autowired
  public ManagementService(
      @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
          String armadilloConfigFile,
      @Value("${spring.security.oauth2.client.provider.molgenis.issuer-uri:#{null}}")
          String issuerUri,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-id:#{null}}")
          String clientId,
      @Value("${spring.security.oauth2.client.registration.molgenis.client-secret:#{null}}")
          String clientSecret,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:#{null}}")
          String deviceIssuerUri,
      @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id:#{null}}")
          String deviceClientId,
      @Autowired BuildProperties buildProperties,
      @Autowired RebootScriptRunner scriptRunner,
      @Autowired JarDownloader jarDownloader,
      @Autowired GithubApi githubApi,
      @Autowired UpdateScriptDownloader updateScriptDownloader,
      @Autowired FileService fileService,
      @Qualifier("jarHome") String jarHome,
      ConfigFile configFile) {
    this.buildProperties = buildProperties;
    this.scriptRunner = scriptRunner;
    this.armadilloConfigFile = armadilloConfigFile;
    this.jarHome = jarHome;
    this.appConfigFile = configFile;
    this.jarDownloader = jarDownloader;
    this.githubApi = githubApi;
    this.updateScriptDownloader = updateScriptDownloader;
    currentOidcDetails =
        OidcDetails.create(issuerUri, clientId, clientSecret, deviceIssuerUri, deviceClientId);
  }

  public void softRestartApplication() {
    ArmadilloServiceApplication.restart();
  }

  private String getProcessName() {
    return ManagementFactory.getRuntimeMXBean().getName();
  }

  String getJavaProcessId(String processName) {
    return Arrays.stream(processName.split("@")).toList().getFirst();
  }

  void throwIfNotSupported(String method) {
    throwWhenRunningInContainer(method);
    throwWhenNotLinuxInProd(method);
  }

  void throwWhenRunningInContainer(String method) throws UnsupportedOperationException {
    if (runningInContainer) {
      throw new UnsupportedOperationException(
          "Cannot execute " + method + "because armadillo is running within a docker container");
    }
  }

  void throwWhenNotLinuxInProd(String method) {
    String os = System.getProperty("os.name");
    if (Objects.equals(this.armadilloMode, "PROD") && !Objects.equals(os, "Linux")) {
      throw new UnsupportedOperationException(
          "Cannot execute "
              + method
              + "because this is only possible to do in production under Linux");
    }
  }

  public void hardRestartApplication() throws IOException {
    throwIfNotSupported("hard restart");
    scriptRunner.runRebootScript(
        updateScriptDownloader.getUpdateScriptPath(),
        "-p",
        armadilloHome,
        "-v",
        "",
        "-m",
        armadilloMode,
        "-i",
        getJavaProcessId(getProcessName()),
        "-c",
        armadilloConfigFile.replace("/application.yml", ""));
  }

  public JsonElement getLastRelease() throws IOException, InterruptedException {
    return githubApi.getLastRelease();
  }

  public Map<String, String> getCurrentOidcConfig() {
    return currentOidcDetails.get();
  }

  // this is intended behaviour
  @java.lang.SuppressWarnings({"javasecurity:S2083"})
  public void deleteJar(String version) {
    String appVersion = buildProperties.getVersion();
    String fileToDelete = getJarPathFromVersion(version);
    if (appVersion.equals(version)) {
      throw new StorageException("Cannot delete file: jar is currently running.");
    } else {
      Path path = Paths.get(fileToDelete);
      try {
        Files.delete(path);
      } catch (NoSuchFileException x) {
        throw new StorageException(format("%s: no such file or directory%n", path));
      } catch (IOException x) {
        throw new StorageException(
            format("Cannot delete file: [%s] because %s.", path.getFileName(), x));
      }
    }
  }

  private String getJarPathFromVersion(String version) {
    return jarHome + File.separator + getJarFromVersion(version);
  }

  String getJarFromVersion(String version) {
    return String.format(ARMADILLO_JAR, version.replace("v", ""));
  }

  boolean isValidVersion(String version) {
    return version.matches("v?\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?") || version.equals("dev");
  }

  public void triggerUpdate(String version) throws IOException {
    throwIfNotSupported("hard restart");
    if (isValidVersion(version)) {
      scriptRunner.runRebootScript(
          updateScriptDownloader.getUpdateScriptPath(),
          "-p",
          armadilloHome,
          "-v",
          version,
          "-m",
          armadilloMode,
          "-i",
          getJavaProcessId(getProcessName()),
          "-u");
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  String getUpdateScriptPath() {
    return updateScriptDownloader.getUpdateScriptPath();
  }

  String getUpdateScriptUrl(String armadilloVersion) {
    return updateScriptDownloader.getUpdateScriptUrl(armadilloVersion);
  }

  public void downloadUpdateScript(String armadilloVersion) throws InterruptedException {
    if (isValidVersion(armadilloVersion)) {
      updateScriptDownloader.downloadUpdateScript(armadilloVersion);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  public Set<String> listLocallyAvailableJars() {
    return listFilesForDir(jarHome).stream()
        .filter(name -> name.endsWith(".jar"))
        .collect(Collectors.toSet());
  }

  private Set<String> listFilesForDir(String dir) {
    return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
        .filter(file -> !file.isDirectory())
        .map(File::getName)
        .collect(Collectors.toSet());
  }

  public Boolean doesJarFit(String version) throws IOException, InterruptedException {
    return jarDownloader.doesJarFit(version);
  }

  public Boolean isValidJar(String version) throws IOException, InterruptedException {
    return jarDownloader.isValidJar(version);
  }

  public SseEmitter downloadArmadilloJar(String version) {
    if (isValidVersion(version)) {
      return jarDownloader.downloadArmadilloJar(version);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified version is not valid");
    }
  }

  public void saveNewOidcConfig(OidcDetails oidcDetails) throws IOException {
    throwIfNotSupported("update oidc config");
    appConfigFile.update(oidcDetails);
    hardRestartApplication();
  }
}
