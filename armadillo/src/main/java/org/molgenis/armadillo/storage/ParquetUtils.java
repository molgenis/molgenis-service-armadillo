package org.molgenis.armadillo.storage;

import static java.lang.Math.min;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;

public class ParquetUtils {
  public static List<Map<String, String>> previewRecords(Path path, int rowLimit, int columnLimit)
      throws IOException {
    List<Map<String, String>> result = new ArrayList<>();
    LocalInputFile file = new LocalInputFile(path);
    try (ParquetFileReader reader = ParquetFileReader.open(file)) {
      MessageType schema = reader.getFooter().getFileMetaData().getSchema();
      MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
      PageReadStore store = reader.readNextRowGroup();
      RecordReader recordReader = columnIO.getRecordReader(store, new GroupRecordConverter(schema));
      int fieldSize = schema.getFields().size();
      for (int i = 0; i < rowLimit; i++) {
        SimpleGroup group = (SimpleGroup) recordReader.read();
        Map<String, String> row = new LinkedHashMap<>();
        for (int fieldIndex = 0; fieldIndex < min(fieldSize, columnLimit); fieldIndex++) {
          try {
            row.put(schema.getFieldName(fieldIndex), group.getValueToString(fieldIndex, 0));
          } catch (Exception e) {
            row.put(schema.getFieldName(fieldIndex), "???");
          }
        }
        result.add(row);
      }
    }
    return result;
  }

  public static Map<String, String> retrieveDimensions(Path path) throws FileNotFoundException {
    LocalInputFile file = new LocalInputFile(path);
    try (ParquetFileReader reader = ParquetFileReader.open(file)) {
      MessageType schema = reader.getFooter().getFileMetaData().getSchema();
      int numberOfColumns = schema.getFields().size();
      long numberOfRows = reader.getRecordCount();
      Map<String, String> dimensions = new HashMap();
      dimensions.put("rows", Long.toString(numberOfRows));
      dimensions.put("columns", Integer.toString(numberOfColumns));
      return dimensions;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
