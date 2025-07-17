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

  @Test
  void testGetDatatypes() throws URISyntaxException, IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());

    Map<String, String> datatypes = ParquetUtils.getDatatypes(path);

    assertNotNull(datatypes);
    assertEquals(3, datatypes.size());

    assertTrue(datatypes.containsKey("id"));
    assertTrue(datatypes.containsKey("age"));
    assertTrue(datatypes.containsKey("name"));

    // Depending on actual schema, adjust expected types below
    assertEquals("INT32", datatypes.get("id")); // or "INT64"
    assertEquals("INT32", datatypes.get("age"));
    assertEquals("BINARY", datatypes.get("name")); // string-like
  }

  @Test
  void testGetColumnMetaData() throws URISyntaxException, IOException {
    Path path =
        Path.of(
            Objects.requireNonNull(this.getClass().getClassLoader().getResource("patient.parquet"))
                .toURI());

    HashMap<String, Map<String, String>> metadata = ParquetUtils.getColumnMetaData(path);

    assertNotNull(metadata);
    assertEquals(3, metadata.size());

    // Validate "id"
    Map<String, String> idMeta = metadata.get("id");
    assertNotNull(idMeta);
    assertEquals("INT32", idMeta.get("type"));
    assertTrue(idMeta.get("missing").matches("\\d+/\\d+")); // e.g., "0/11"
    assertFalse(idMeta.containsKey("levels")); // INT32 should not have levels

    // Validate "age"
    Map<String, String> ageMeta = metadata.get("age");
    assertNotNull(ageMeta);
    assertEquals("INT32", ageMeta.get("type"));
    assertTrue(ageMeta.get("missing").matches("\\d+/\\d+"));
    assertFalse(ageMeta.containsKey("levels")); // No levels for non-BINARY

    // Validate "name"
    Map<String, String> nameMeta = metadata.get("name");
    assertNotNull(nameMeta);
    assertEquals("BINARY", nameMeta.get("type"));
    assertTrue(nameMeta.get("missing").matches("\\d+/\\d+"));
    if (nameMeta.containsKey("levels")) {
      assertTrue(nameMeta.get("levels").startsWith("["));
    }
  }

  @Test
  void testGetColumnMetaDataThrowsOnInvalidFile() {
    Path invalidPath = Path.of("non_existent.parquet");

    assertThrows(IOException.class, () -> ParquetUtils.getColumnMetaData(invalidPath));
  }

  @Test
  void testProcessRowsWithAllValuesPresent() {
    Map<String, String> datatypes = Map.of("col", "INT32");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();
    List<String> columns = List.of("col");
    long numberOfRows = 1;

    Group group = mock(Group.class);
    when(group.getValueToString(0, 0)).thenReturn("123");

    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("col")).thenReturn(0);

    RecordReader<Group> recordReader =
        new RecordReader<>() {
          private int count = 0;

          public Group read() {
            return count++ == 0 ? group : null;
          }
        };

    ParquetUtils.processRows(
        recordReader,
        numberOfRows,
        schema,
        columns,
        datatypes,
        columnMetaData,
        rawLevels,
        levelCounts);

    assertEquals("0/1", columnMetaData.get("col").get("missing"));
    assertEquals("", columnMetaData.get("col").get("levels"));
  }

  @Test
  void testProcessRowsWithMissingValue() {
    Map<String, String> datatypes = Map.of("col", "INT32");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();
    List<String> columns = List.of("col");
    long numberOfRows = 1;

    Group group = mock(Group.class);
    when(group.getValueToString(0, 0)).thenReturn(""); // empty = missing

    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("col")).thenReturn(0);

    RecordReader<Group> recordReader =
        new RecordReader<>() {
          private int count = 0;

          public Group read() {
            return count++ == 0 ? group : null;
          }
        };

    ParquetUtils.processRows(
        recordReader,
        numberOfRows,
        schema,
        columns,
        datatypes,
        columnMetaData,
        rawLevels,
        levelCounts);

    assertEquals("1/1", columnMetaData.get("col").get("missing"));
  }

  @Test
  void testProcessRowsBinaryLevels() {
    Map<String, String> datatypes = Map.of("status", "BINARY");
    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    HashMap<String, Integer> levelCounts = new HashMap<>();
    List<String> columns = List.of("status");
    long numberOfRows = 2;

    Group group1 = mock(Group.class);
    when(group1.getValueToString(0, 0)).thenReturn("active");

    Group group2 = mock(Group.class);
    when(group2.getValueToString(0, 0)).thenReturn("inactive");

    MessageType schema = mock(MessageType.class);
    when(schema.getFieldIndex("status")).thenReturn(0);

    RecordReader<Group> recordReader =
        new RecordReader<>() {
          private int count = 0;

          public Group read() {
            return switch (count++) {
              case 0 -> group1;
              case 1 -> group2;
              default -> null;
            };
          }
        };

    ParquetUtils.processRows(
        recordReader,
        numberOfRows,
        schema,
        columns,
        datatypes,
        columnMetaData,
        rawLevels,
        levelCounts);

    assertEquals("0/2", columnMetaData.get("status").get("missing"));
    assertTrue(rawLevels.get("status").contains("active"));
    assertTrue(rawLevels.get("status").contains("inactive"));
    assertEquals(2, levelCounts.get("status"));
  }

  @Test
  void testAddLevelsToMetaData_shouldAddLevelsWhenNotUniqueAndBinary() {
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    rawLevels.put("col", new ArrayList<>(List.of("A", "B")));

    HashMap<String, Integer> levelCounts = new HashMap<>();
    levelCounts.put("col", 1); // 1 / 5 = 0.2 < 0.3 → not unique

    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    columnMetaData.put("col", new HashMap<>(Map.of("type", "BINARY", "missing", "0/5")));

    ParquetUtils.addLevelsToMetaData(rawLevels, levelCounts, 5, columnMetaData);

    assertEquals("[A, B]", columnMetaData.get("col").get("levels"));
  }

  @Test
  void testAddLevelsToMetaData_shouldRemoveLevelsWhenUnique() {
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    rawLevels.put("col", new ArrayList<>(List.of("A", "B", "C")));

    HashMap<String, Integer> levelCounts = new HashMap<>();
    levelCounts.put("col", 5); // 5 / 5 = 1.0 → unique

    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    columnMetaData.put(
        "col", new HashMap<>(Map.of("type", "BINARY", "missing", "0/5", "levels", "[A, B, C]")));

    ParquetUtils.addLevelsToMetaData(rawLevels, levelCounts, 5, columnMetaData);

    assertFalse(columnMetaData.get("col").containsKey("levels")); // Should be removed
  }

  @Test
  void testAddLevelsToMetaData_shouldSkipNonBinaryColumns() {
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    rawLevels.put("col", new ArrayList<>(List.of("1", "2")));

    HashMap<String, Integer> levelCounts = new HashMap<>();
    levelCounts.put("col", 1); // doesn't matter, not BINARY

    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    columnMetaData.put(
        "col", new HashMap<>(Map.of("type", "INT32", "missing", "0/5", "levels", "[1, 2]")));

    ParquetUtils.addLevelsToMetaData(rawLevels, levelCounts, 5, columnMetaData);

    assertFalse(columnMetaData.get("col").containsKey("levels")); // Should be removed
  }

  @Test
  void testAddLevelsToMetaData_handlesEmptyLevelsGracefully() {
    HashMap<String, List<String>> rawLevels = new HashMap<>();
    rawLevels.put("col", new ArrayList<>());

    HashMap<String, Integer> levelCounts = new HashMap<>();
    levelCounts.put("col", 0);

    HashMap<String, Map<String, String>> columnMetaData = new HashMap<>();
    columnMetaData.put("col", new HashMap<>(Map.of("type", "BINARY", "missing", "0/2")));

    ParquetUtils.addLevelsToMetaData(rawLevels, levelCounts, 2, columnMetaData);

    assertEquals("[]", columnMetaData.get("col").get("levels")); // empty, but added
  }
}
