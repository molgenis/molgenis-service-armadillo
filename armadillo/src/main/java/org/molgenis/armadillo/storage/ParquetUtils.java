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
                if (Objects.equals(value, "NA")) {
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
