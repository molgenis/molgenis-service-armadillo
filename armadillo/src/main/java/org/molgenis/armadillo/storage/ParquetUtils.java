package org.molgenis.armadillo.storage;

import static java.lang.Integer.parseInt;

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

public class ParquetUtils {
  private static final String MISSING = "missing";
  private static final String LEVELS = "levels";
  private static final String BINARY_TYPE = "binary";
  private static final String TYPE = "type";
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

  public static HashMap<String, Map<String, String>> getColumnMetaData(Path path)
      throws IOException {
    HashMap<String, List<String>> raw_levels = new LinkedHashMap<>();
    HashMap<String, Integer> level_counts = new LinkedHashMap<>();
    Map<String, String> datatypes = getDatatypes(path);
    HashMap<String, Map<String, String>> columnMetaData = new LinkedHashMap<>();
    try (ParquetFileReader reader = getFileReader(path)) {
      long numberOfRows = reader.getRecordCount();
      MessageType schema = getSchemaFromReader(reader);
      RecordReader<Group> recordReader = getRecordReader(schema, reader);
      List<String> columns = getColumnsFromSchema(schema);
      for (int i = 0; i < numberOfRows; i++) {
        SimpleGroup group = (SimpleGroup) recordReader.read();
        columns.forEach(
            column -> {
              if (!columnMetaData.containsKey(column)) {
                Map<String, String> metaDataForColumn = new LinkedHashMap<>();
                metaDataForColumn.put(TYPE, datatypes.get(column));
                metaDataForColumn.put(MISSING, "0/" + numberOfRows);
                metaDataForColumn.put(LEVELS, "");
                level_counts.put(column, 0);
                raw_levels.put(column, new ArrayList<>());
                columnMetaData.put(column, metaDataForColumn);
              }

              try {
                var value = group.getValueToString(schema.getFieldIndex(column), 0);
                if (isEmpty(value)) {
                  countMissingValue(columnMetaData, column);
                }
                if (Objects.equals(datatypes.get(column), BINARY_TYPE)) {
                  try {
                    if (!isEmpty(value) && !raw_levels.get(column).contains(value)) {
                      List<String> currentLevels = raw_levels.get(column);
                      currentLevels.add(value);
                      countLevelValue(raw_levels, level_counts, column, value);
                    }
                  } catch (Exception ignored) {
                  }
                }
              } catch (Exception e) {
                if (columnMetaData.containsKey(column)) {
                  countMissingValue(columnMetaData, column);
                }
              }
            });
      }
      addLevelsToMetaData(raw_levels, level_counts, numberOfRows, columnMetaData);
    }
    return columnMetaData;
  }

  private static void countLevelValue(
      HashMap<String, List<String>> raw_levels,
      HashMap<String, Integer> level_counts,
      String column,
      String levelToAdd) {
    List<String> currentLevels = raw_levels.get(column);
    currentLevels.add(levelToAdd);
    raw_levels.put(column, currentLevels);
    level_counts.put(column, level_counts.get(column) + 1);
  }

  private static void countMissingValue(
      HashMap<String, Map<String, String>> columnMetaData, String column) {
    Map<String, String> metaDataForThisColumn = columnMetaData.get(column);
    String[] splitValue = metaDataForThisColumn.get(MISSING).split("/");
    int currentCount = parseInt(splitValue[0]) + 1;
    String total = "/" + splitValue[1];
    metaDataForThisColumn.put(MISSING, currentCount + total);
  }

  private static void addLevelsToMetaData(
      HashMap<String, List<String>> raw_levels,
      HashMap<String, Integer> level_counts,
      long numberOfRows,
      HashMap<String, Map<String, String>> columnMetaData) {
    raw_levels.forEach(
        (column, column_levels) -> {
          if (!isUnique(level_counts.get(column), numberOfRows)
              && columnMetaData.get(column).get(TYPE).equals(BINARY_TYPE)) {
            columnMetaData.get(column).put(LEVELS, String.valueOf(column_levels));
          } else {
            columnMetaData.get(column).remove(LEVELS);
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
