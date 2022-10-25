package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ParquetUtilsTest {
  @Test
  public void testParquetPreview() throws IOException, URISyntaxException {
    Path path = Path.of(this.getClass().getClassLoader().getResource("patient.parquet").toURI());
    List<Map<String, String>> preview = ParquetUtils.previewRecords(path, 10, 10);
    assertEquals("Patient1", preview.get(0).get("name"));
  }
}
