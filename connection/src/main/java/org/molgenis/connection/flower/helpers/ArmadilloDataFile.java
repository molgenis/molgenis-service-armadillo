package org.molgenis.connection.flower.helpers;

import java.io.File;
import org.apache.commons.io.FileUtils;

public class ArmadilloDataFile {
  /***
   * Helper file for FlowerServerConnection
   * */
  String filePath;

  String[] pathComponents;

  public ArmadilloDataFile(String filePath) {
    this.filePath = filePath;
    this.pathComponents = filePath.split("/");
  }

  private int getPathLength() {
    return pathComponents.length;
  }

  private String getPathComponentByIndex(int index) {
    return pathComponents[index];
  }

  String getProjectName() {
    return getPathComponentByIndex(getPathLength() - 3);
  }

  String getFolderName() {
    return getPathComponentByIndex(getPathLength() - 2);
  }

  String getFileName() {
    return getPathComponentByIndex(getPathLength() - 1);
  }

  public String getArmadilloFilePath() {
    return String.format("%s/%s/%s", getProjectName(), getFolderName(), getFileName());
  }

  public long getSize() {
    File dataFile = new File(filePath);
    return FileUtils.sizeOf(dataFile);
  }
}
