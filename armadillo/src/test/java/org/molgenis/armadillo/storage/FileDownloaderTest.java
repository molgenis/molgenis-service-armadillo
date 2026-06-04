package org.molgenis.armadillo.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.molgenis.armadillo.storage.FileDownloader.getPercentage;
import static org.molgenis.armadillo.storage.FileDownloader.processFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDownloaderTest {

  @Test
  void getPercentage_should_calculate_correctly() {
    assertThat(getPercentage(50, 100)).isEqualTo(50);
    assertThat(getPercentage(1, 100)).isEqualTo(1);
    assertThat(getPercentage(100, 100)).isEqualTo(100);
  }

  @Test
  void processFile_should_write_bytes_and_report_progress(@TempDir Path tempDir) throws Exception {
    byte[] data = "hello world".getBytes();
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data));
    File out = tempDir.resolve("out.bin").toFile();
    List<Long> progressUpdates = new ArrayList<>();

    try (FileOutputStream fos = new FileOutputStream(out)) {
      processFile(fos, in, data.length, progressUpdates::add);
    }

    assertThat(out).hasContent("hello world");
    assertThat(progressUpdates).isNotEmpty();
  }
}
