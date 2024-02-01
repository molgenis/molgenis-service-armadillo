package org.molgenis.armadillo.info;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.io.File;
import org.springframework.stereotype.Component;

@Component
public class FileMetrics implements MeterBinder {

  private static final String PATH = System.getProperty("user.dir");

  @Override
  public void bindTo(MeterRegistry registry) {
    File folder = new File(PATH);
    File[] listOfFiles = folder.listFiles();

    int fileCount = 0;
    int dirCount = 0;

    if (listOfFiles != null) {
      for (File file : listOfFiles) {
        if (file.isFile()) {
          fileCount++;
        } else if (file.isDirectory()) {
          dirCount++;
        }
      }
    }

    Gauge.builder("user.files.count", fileCount, Integer::doubleValue)
        .description("Number of files in the current directory")
        .baseUnit("files")
        .tags("path", PATH)
        .register(registry);

    Gauge.builder("user.directories.count", dirCount, Integer::doubleValue)
        .description("Number of directories in the current directory")
        .baseUnit("user.directory.count")
        .tags("path", PATH)
        .register(registry);
  }
}
