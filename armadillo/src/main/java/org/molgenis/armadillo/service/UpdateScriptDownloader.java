package org.molgenis.armadillo.service;

import static java.lang.String.format;

import java.io.File;
import org.molgenis.armadillo.storage.FileDownloader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateScriptDownloader {

  private static final String REBOOT_SCRIPT = "armadillo-reboot.sh";
  private static final String REBOOT_SCRIPT_URL =
      "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/%s/scripts/install/%s";

  private final String jarHome;

  public UpdateScriptDownloader(@Qualifier("jarHome") String jarHome) {
    this.jarHome = jarHome;
  }

  public String getUpdateScriptPath() {
    return format("%s/%s", jarHome, REBOOT_SCRIPT);
  }

  String getUpdateScriptUrl(String armadilloVersion) {
    return String.format(REBOOT_SCRIPT_URL, getScriptVersionTag(armadilloVersion), REBOOT_SCRIPT);
  }

  private String getScriptVersionTag(String version) {
    // if script not available yet on current release:
    String scriptVersionTag = "9f0c9b53e773ac54940d29d160712c6e61a485b6";
    if (!version.equals("dev")) {
      version = version.replace("v", "");
    }
    String[] versionSplit = version.split("\\.");
    try {
      if (Integer.parseInt(versionSplit[0]) > 5
          || (Integer.parseInt(versionSplit[0]) == 5 && Integer.parseInt(versionSplit[1]) >= 15)) {
        scriptVersionTag = "refs/tags/v" + version;
      }
    } catch (NumberFormatException ignored) {
      // when dev
    }
    return scriptVersionTag;
  }

  public void downloadUpdateScript(String armadilloVersion) throws InterruptedException {
    String updateScriptPath = getUpdateScriptPath();
    FileDownloader.downloadFile(getUpdateScriptUrl(armadilloVersion), updateScriptPath);
    File script = new File(updateScriptPath);
    if (!script.setExecutable(true, false)) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to set file as executable: " + updateScriptPath);
    }
  }
}
