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

public class ParquetUtils {
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
                row.put(column, "NA");
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
    // TODO: doesn't work perfect, see
    // http://localhost:8080/storage/projects/datashield/objects/factor_levels%2FFACTOR_LEVELS3.parquet/metadata
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

  public static HashMap<String, List<String>> getLevels(Path path) throws IOException {
    HashMap<String, List<String>> raw_levels = new LinkedHashMap<>();
    HashMap<String, Integer> level_counts = new LinkedHashMap<>();
    Map<String, String> datatypes = getDatatypes(path);
    try (ParquetFileReader reader = getFileReader(path)) {
      long numberOfRows = reader.getRecordCount();
      MessageType schema = getSchemaFromReader(reader);
      RecordReader<Group> recordReader = getRecordReader(schema, reader);
      List<String> columns = getColumnsFromSchema(schema);

      for (int i = 0; i < numberOfRows; i++) {
        SimpleGroup group = (SimpleGroup) recordReader.read();
        columns.forEach(
            column -> {
              try {
                String value = group.getValueToString(schema.getFieldIndex(column), 0);
                if (!raw_levels.containsKey(column)) {
                  raw_levels.put(column, new ArrayList<>(Arrays.asList(value)));
                  level_counts.put(column, 1);
                }
                // if column is binary, if value in row not in list, add it
                if (Objects.equals(datatypes.get(column), "BINARY")) {
                  try {
                    if (!isEmpty(value) && !raw_levels.get(column).contains(value)) {
                      // add value to raw_levels of column
                      List<String> currentLevels = raw_levels.get(column);
                      currentLevels.add(value);
                      raw_levels.put(column, currentLevels);
                      level_counts.put(column, level_counts.get(column) + 1);
                    }
                  } catch (Exception ignored) {
                  }
                }
              } catch (Exception ignored) {
              }
            });
      }
      HashMap<String, List<String>> levels = (HashMap<String, List<String>>) raw_levels.clone();
      // display raw_levels if the number of unique raw_levels is <= 0.3 / length of data frame
      raw_levels.forEach(
          (column, column_levels) -> {
            if (isUnique(level_counts.get(column), numberOfRows)) {
              levels.remove(column);
            }
          });
      return levels;
    }
  }

  static boolean isUnique(int occurrences, long totalRows) {
    return ((double) occurrences / totalRows) >= 0.3;
  }

  static boolean isEmpty(String value) {
    return value == null || value.isEmpty() || Objects.equals(value, "NA");
  }

  public static Map<String, Map<String, Integer>> getMissingData(Path path) throws IOException {
    Map<String, Map<String, Integer>> missings = new LinkedHashMap<>();
    try (ParquetFileReader reader = getFileReader(path)) {
      long numberOfRows = reader.getRecordCount();
      MessageType schema = getSchemaFromReader(reader);
      RecordReader<Group> recordReader = getRecordReader(schema, reader);
      List<String> columns = getColumnsFromSchema(schema);

      for (int i = 0; i < numberOfRows; i++) {
        SimpleGroup group = (SimpleGroup) recordReader.read();
        columns.forEach(
            column -> {
              if (!missings.containsKey(column)) {
                Map<String, Integer> missingInfo = new LinkedHashMap<>();
                missingInfo.put("count", 0);
                missingInfo.put("total", (int) numberOfRows);
                missings.put(column, missingInfo);
              }
              Map<String, Integer> missingInfo = missings.get(column);
              Integer currentValue = missingInfo.get("count");
              try {
                var value = group.getValueToString(schema.getFieldIndex(column), 0);
                if (isEmpty(value)) {
                  missingInfo.put("count", currentValue + 1);
                  missings.put(column, missingInfo);
                }
              } catch (Exception e) {
                if (missings.containsKey(column)) {
                  missingInfo.put("count", currentValue + 1);
                  missings.put(column, missingInfo);
                }
              }
            });
      }
    }
    return missings;
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
