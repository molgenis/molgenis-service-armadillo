package org.molgenis.armadillo.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
  public static String getServerTime() {
    LocalDateTime now = LocalDateTime.now();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    return now.format(formatter);
  }
}
