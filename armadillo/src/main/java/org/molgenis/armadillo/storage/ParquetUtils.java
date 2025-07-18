package org.molgenis.armadillo.storage;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.molgenis.armadillo.model.ArmadilloColumnMetaData;

public class ParquetUtils {
  private static final String MISSING = "missing";
  private static final String LEVELS = "levels";
  private static final String TYPE = "type";
  private static final String BINARY_TYPE = "BINARY";
  private static final String NA_VALUE = "NA";

  public static List<Map<String, String>> previewRecords(
      Path path, int rowLimit, int columnLimit, String[] variables) throws IOException {
    List<Map<String, String>> result = new ArrayList<>();
    try (ParquetFileReader reader = getFileReader(path)) {
      MessageType schema = getSchemaFromReader(reader);
      RecordReader<Group> recordReader = getRecordReader(schema, reader);
      List<String> columns = getColumnsFromSchema(schema);

      for (int i = 0; i < rowLimit; i++) {
        result.add(getPreviewRow(recordReader, schema, columns, columnLimit, variables));
      }
    }
    return result;
  }

  private static Map<String, String> getPreviewRow(
      RecordReader<Group> recordReader,
      MessageType schema,
      List<String> columns,
      int columnLimit,
      String[] variables) {
    AtomicInteger colCount = new AtomicInteger();
    SimpleGroup group = (SimpleGroup) recordReader.read();
    Map<String, String> row = new LinkedHashMap<>();
    columns.forEach(
        column -> {
          if (colCount.get() < columnLimit) {
            if (variables.length == 0 || Arrays.stream(variables).toList().contains(column)) {
              colCount.getAndIncrement();
              try {
                row.put(column, group.getValueToString(schema.getFieldIndex(column), 0));
              } catch (Exception e) {
                row.put(column, NA_VALUE);
              }
            }
          }
        });
    return row;
  }

  private static List<String> getColumnsFromSchema(MessageType schema) {
    return IntStream.range(0, schema.getFieldCount()).mapToObj(schema::getFieldName).toList();
  }

  private static MessageType getSchemaFromReader(ParquetFileReader reader) {
    return reader.getFooter().getFileMetaData().getSchema();
  }

  private static RecordReader<Group> getRecordReader(MessageType schema, ParquetFileReader reader)
      throws IOException {
    MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
    PageReadStore store = reader.readNextRowGroup();
    return columnIO.getRecordReader(store, new GroupRecordConverter(schema));
  }

  private static ParquetFileReader getFileReader(Path path) throws IOException {
    LocalInputFile file = new LocalInputFile(path);
    return ParquetFileReader.open(file);
  }

  public static List<String> getColumns(Path path) throws IOException {
    try (ParquetFileReader reader = getFileReader(path)) {
      var schema = getSchemaFromReader(reader);
      return getColumnsFromSchema(schema);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, String> getDatatypes(Path path) throws IOException {
    try (ParquetFileReader reader = getFileReader(path)) {
      List<Type> schema = getSchemaFromReader(reader).getFields();
      Map<String, String> datatypes = new LinkedHashMap<>();
      schema.forEach(
          (field) -> {
            datatypes.put(
                field.getName(), ((PrimitiveType) field).getPrimitiveTypeName().toString());
          });
      return datatypes;
    }
  }

  public static HashMap<String, ArmadilloColumnMetaData> getColumnMetaData(Path path)
      throws IOException {
    HashMap<String, Set<String>> rawLevels = new LinkedHashMap<>();
    HashMap<String, Integer> levelCounts = new LinkedHashMap<>();
    Map<String, String> datatypes = getDatatypes(path);
    HashMap<String, ArmadilloColumnMetaData> columnMetaData = new LinkedHashMap<>();

    try (ParquetFileReader reader = getFileReader(path)) {
      long numberOfRows = reader.getRecordCount();
      MessageType schema = getSchemaFromReader(reader);
      RecordReader<Group> recordReader = getRecordReader(schema, reader);
      List<String> columns = getColumnsFromSchema(schema);

      processRows(
          recordReader,
          numberOfRows,
          schema,
          columns,
          datatypes,
          columnMetaData,
          rawLevels,
          levelCounts);

      addLevelsToMetaData(rawLevels, levelCounts, numberOfRows, columnMetaData);
    }

    return columnMetaData;
  }

  static void processRows(
      RecordReader<Group> recordReader,
      long numberOfRows,
      MessageType schema,
      List<String> columns,
      Map<String, String> datatypes,
      HashMap<String, ArmadilloColumnMetaData> columnMetaData,
      HashMap<String, Set<String>> rawLevels,
      HashMap<String, Integer> levelCounts) {
    for (int i = 0; i < numberOfRows; i++) {
      Group group = recordReader.read();
      for (String column : columns) {
        initializeColumnMetadata(
            column, datatypes, columnMetaData, rawLevels, levelCounts, numberOfRows);
        handleColumnValue(group, schema, column, datatypes, columnMetaData, rawLevels, levelCounts);
      }
    }
  }

  static void initializeColumnMetadata(
      String column,
      Map<String, String> datatypes,
      HashMap<String, ArmadilloColumnMetaData> columnMetaData,
      HashMap<String, Set<String>> rawLevels,
      HashMap<String, Integer> levelCounts,
      long numberOfRows) {
    if (!columnMetaData.containsKey(column)) {
      ArmadilloColumnMetaData armadilloColumnMetaData =
          ArmadilloColumnMetaData.create(datatypes.get(column));
      armadilloColumnMetaData.setTotal(numberOfRows);
      levelCounts.put(column, 0);
      rawLevels.put(column, new HashSet<>());
      columnMetaData.put(column, armadilloColumnMetaData);
    }
  }

  static void handleColumnValue(
      Group group,
      MessageType schema,
      String column,
      Map<String, String> datatypes,
      HashMap<String, ArmadilloColumnMetaData> columnMetaData,
      HashMap<String, Set<String>> rawLevels,
      HashMap<String, Integer> levelCounts) {
    try {
      String value = group.getValueToString(schema.getFieldIndex(column), 0);

      if (isEmpty(value)) {
        columnMetaData.get(column).countMissingValue();
      } else if (Objects.equals(datatypes.get(column), BINARY_TYPE)) {
        handleBinaryLevel(column, value, rawLevels, levelCounts);
      }

    } catch (Exception e) {
      columnMetaData.get(column).countMissingValue();
    }
  }

  private static void handleBinaryLevel(
      String column,
      String value,
      HashMap<String, Set<String>> rawLevels,
      HashMap<String, Integer> levelCounts) {
    try {
      Set<String> currentLevels = rawLevels.get(column);
      if (!currentLevels.contains(value)) {
        currentLevels.add(value);
        countLevelValue(rawLevels, levelCounts, column, value);
      }
    } catch (Exception ignored) {
    }
  }

  private static void countLevelValue(
      HashMap<String, Set<String>> raw_levels,
      HashMap<String, Integer> level_counts,
      String column,
      String levelToAdd) {
    Set<String> currentLevels = raw_levels.get(column);
    currentLevels.add(levelToAdd);
    raw_levels.put(column, currentLevels);
    level_counts.put(column, level_counts.get(column) + 1);
  }

  static void addLevelsToMetaData(
      HashMap<String, Set<String>> raw_levels,
      HashMap<String, Integer> level_counts,
      long numberOfRows,
      HashMap<String, ArmadilloColumnMetaData> columnMetaData) {
    raw_levels.forEach(
        (column, column_levels) -> {
          if (!isUnique(level_counts.get(column), numberOfRows)
              && columnMetaData.get(column).getType().equals(BINARY_TYPE)) {
            columnMetaData.get(column).setPossibleLevels(column_levels);
          } else {
            columnMetaData.get(column).setPossibleLevels(null);
          }
        });
  }

  static boolean isUnique(int occurrences, long totalRows) {
    if (totalRows == 0) {
      throw new ArithmeticException("Number of rows is 0, cannot divide by 0");
    }
    return ((double) occurrences / totalRows) >= 0.3;
  }

  static boolean isEmpty(String value) {
    return value == null || value.isEmpty() || Objects.equals(value, NA_VALUE);
  }

  public static Map<String, String> retrieveDimensions(Path path) throws FileNotFoundException {
    try (ParquetFileReader reader = getFileReader(path)) {
      MessageType schema = getSchemaFromReader(reader);
      int numberOfColumns = schema.getFields().size();
      long numberOfRows = reader.getRecordCount();
      Map<String, String> dimensions = new HashMap<>();
      dimensions.put("rows", Long.toString(numberOfRows));
      dimensions.put("columns", Integer.toString(numberOfColumns));
      return dimensions;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
