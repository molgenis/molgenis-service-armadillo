package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
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

  @Test
  void testInitializeColumnMetadataCreatesExpectedStructure() {
    Map<String, String> datatypes = Map.of("myColumn", "BINARY");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();

    ParquetUtils.initializeColumnMetadata(
        "myColumn", datatypes, columnMetaData, rawLevels, levelCounts, 5L);

    assertTrue(columnMetaData.containsKey("myColumn"));
    assertEquals("BINARY", columnMetaData.get("myColumn").get("type"));
    assertEquals("0/5", columnMetaData.get("myColumn").get("missing"));
    assertEquals("", columnMetaData.get("myColumn").get("levels"));
    assertEquals(0, levelCounts.get("myColumn"));
    assertEquals(List.of(), rawLevels.get("myColumn"));
  }

  @Test
  void testHandleColumnValueCountsMissingWhenEmpty() {
    Map<String, String> datatypes = Map.of("col", "BINARY");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();

    columnMetaData.put(
        "col", new HashMap<>(Map.of("type", "BINARY", "missing", "0/1", "levels", "")));
    rawLevels.put("col", new ArrayList<>());
    levelCounts.put("col", 0);

    // Mock Group
    Group group = mock(Group.class);
    when(group.getValueToString(anyInt(), anyInt())).thenReturn(""); // simulate empty value

    // Mock MessageType and getFieldIndex
    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("col")).thenReturn(0);

    ParquetUtils.handleColumnValue(
        group, schema, "col", datatypes, columnMetaData, rawLevels, levelCounts);

    assertEquals("1/1", columnMetaData.get("col").get("missing"));
  }

  @Test
  void testHandleColumnValueAddsBinaryLevel() {
    Map<String, String> datatypes = Map.of("col", "BINARY");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();
    columnMetaData.put(
        "col", new HashMap<>(Map.of("type", "BINARY", "missing", "0/1", "levels", "")));
    rawLevels.put("col", new ArrayList<>());
    levelCounts.put("col", 0);

    SimpleGroup group = mock(SimpleGroup.class);
    when(group.getValueToString(anyInt(), anyInt())).thenReturn("LEVEL_A");

    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("col")).thenReturn(0);

    ParquetUtils.handleColumnValue(
        group, schema, "col", datatypes, columnMetaData, rawLevels, levelCounts);

    assertTrue(rawLevels.get("col").contains("LEVEL_A"));
    assertEquals(1, levelCounts.get("col"));
  }

  @Test
  void testProcessRowsWithSingleRow() throws Exception {
    // Prepare mocks and test data
    Map<String, String> datatypes = Map.of("col1", "BINARY", "col2", "INT32");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();
    List<String> columns = List.of("col1", "col2");
    long numberOfRows = 1;

    // Mock Group to return values for each column index
    Group group = mock(Group.class);
    when(group.getValueToString(0, 0)).thenReturn("level1"); // For "col1"
    when(group.getValueToString(1, 0)).thenReturn("42"); // For "col2"

    // Mock MessageType and getFieldIndex for columns
    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("col1")).thenReturn(0);
    when(schema.getFieldIndex("col2")).thenReturn(1);

    // Create a mocked RecordReader<Group> that returns the mock Group once, then null
    RecordReader<Group> recordReader =
        new RecordReader<>() {
          private int readCount = 0;

          @Override
          public Group read() {
            if (readCount == 0) {
              readCount++;
              return group;
            }
            return null;
          }
        };

    // Call the package-private processRows method (assumed package-private)
    ParquetUtils.processRows(
        recordReader,
        numberOfRows,
        schema,
        columns,
        datatypes,
        columnMetaData,
        rawLevels,
        levelCounts);

    // Assert expected metadata results
    assertTrue(columnMetaData.containsKey("col1"));
    assertTrue(columnMetaData.containsKey("col2"));

    // Check missing counts (should be "0/1" initially)
    assertEquals("0/1", columnMetaData.get("col1").get("missing"));
    assertEquals("0/1", columnMetaData.get("col2").get("missing"));

    // For col1 (BINARY), level should be recorded
    assertTrue(rawLevels.get("col1").contains("level1"));
    assertEquals(1, levelCounts.get("col1"));

    // For col2 (INT32), no levels should be added (only missing count)
    assertTrue(rawLevels.get("col2") == null || rawLevels.get("col2").isEmpty());
  }
}
