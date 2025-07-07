package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class ParquetUtilsTest {
  @Test
  public void testParquetPreview() throws IOException, URISyntaxException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());
    List<Map<String, String>> preview = ParquetUtils.previewRecords(path, 10, 10, new String[0]);
    assertEquals("Patient1", preview.get(0).get("name"));
    assertEquals(preview.size(), 10);
  }

  @Test
  void testRetrieveDimensions() throws URISyntaxException, FileNotFoundException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());
    Map<String, String> dimensions = ParquetUtils.retrieveDimensions(path);
    assertEquals("3", dimensions.get("columns"));
    assertEquals("11", dimensions.get("rows"));
  }

  @Test
  void testGetColumns() throws URISyntaxException, IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());
    assertEquals(List.of("id", "age", "name"), ParquetUtils.getColumns(path));
  }

  @Test
  void testPreviewWithVariables() throws URISyntaxException, IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());
    List<Map<String, String>> preview =
        ParquetUtils.previewRecords(path, 10, 10, new String[] {"age"});
    assertEquals("24", preview.get(0).get("age"));
    assertNull(preview.get(0).get("name"));
  }

  @Test
  void testIsUniqueAboveThreshold() {
    assertTrue(ParquetUtils.isUnique(30, 100)); // 0.3
    assertTrue(ParquetUtils.isUnique(40, 100)); // 0.4
  }

  @Test
  void testIsUniqueBelowThreshold() {
    assertFalse(ParquetUtils.isUnique(29, 100)); // 0.29
    assertFalse(ParquetUtils.isUnique(0, 100)); // 0.0
  }

  @Test
  void testIsUniqueEdgeCaseZeroRows() {
    assertThrows(ArithmeticException.class, () -> ParquetUtils.isUnique(1, 0));
  }

  // --- Tests for isEmpty ---

  @Test
  void testIsEmptyWithNull() {
    assertTrue(ParquetUtils.isEmpty(null));
  }

  @Test
  void testIsEmptyWithEmptyString() {
    assertTrue(ParquetUtils.isEmpty(""));
  }

  @Test
  void testIsEmptyWithNA() {
    assertTrue(ParquetUtils.isEmpty("NA"));
  }

  @Test
  void testIsEmptyWithNonEmptyString() {
    assertFalse(ParquetUtils.isEmpty("Hello"));
  }

  @Test
  void testIsEmptyWithWhitespaceOnly() {
    assertFalse(ParquetUtils.isEmpty("   "));
  }
}
