package org.molgenis.armadillo.storage;

import java.io.File;

public class DiskSpaceChecker {
  private DiskSpaceChecker() {}

  public static long getAvailableDiskspace() {
    File drive = new File("/");
    return drive.getUsableSpace();
  }

  public static boolean fitsOnDisk(long fileSize) {
    long usableSpace = getAvailableDiskspace();
    return usableSpace > fileSize * 2L;
  }
}
