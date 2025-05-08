package org.molgenis.armadillo.storage;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;
import org.springframework.web.multipart.MultipartFile;

public class CharacterSeparatedFile {
  String STRING = "string";
  String DOUBLE = "double";
  MultipartFile file;
  char separator = ',';
  Schema schema;
  String[] header;
  List<String> types;

  public CharacterSeparatedFile(MultipartFile file) throws IOException, CsvValidationException {
    this.file = file;
    // test if , is real separator
    CSVReader reader = this.getReader();
    this.setHeader(reader.readNext());
    this.implySeparatorFromHeader(header);
    // if , is not the separator, change it
    if (separator != ',') {
      reader = this.getReader();
      this.setHeader(reader.readNext());
    }
    this.types = this.getTypesFromData(reader);
    this.setSchema(this.createSchemaFromTypes(this.types, this.header));
  }

  public void setSeparator(char separator) {
    this.separator = separator;
  }

  public void setHeader(String[] header) {
    this.header = header;
  }

  public void setHeaderColname(String colname, int index) {
    this.header[index] = colname;
  }

  public void implySeparatorFromHeader(String[] header) throws CsvValidationException {
    if (header.length == 1) {
      String headerLine = header[0];
      if (headerLine.split(";").length > 1) {
        this.setSeparator(';');
      } else if (headerLine.split("\t").length > 1) {
        this.setSeparator('\t');
      } else if (headerLine.split("\\|").length > 1) {
        this.setSeparator('|');
      } else {
        // separator must be , ; \t or |
        throw new CsvValidationException(
            String.format(
                "Unsupported separator in file [%s] with header: [%s]",
                this.file.getOriginalFilename(), headerLine));
      }
    }
  }

  public CSVReader getReader() throws IOException {
    CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
    return new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
        .withCSVParser(parser)
        .build();
  }

  String getTypeOfCell(String cell) {
    try {
      Double.parseDouble(cell);
      return DOUBLE;
    } catch (NumberFormatException e) {
      return STRING;
    }
  }

  public List<String> getTypesFromData(CSVReader reader)
      throws IOException, CsvValidationException {
    String[] line;
    String[] types = new String[this.header.length];
    while ((line = reader.readNext()) != null && reader.getLinesRead() < 100) {
      int i = 0;
      for (String value : line) {
        // determine type
        // if value is NA, it's not a string by definition
        if (!value.isEmpty()) {
          String type = getTypeOfCell(value);
          // only set to double if is not string yet, because if one value is not double, the cell
          // cannot be double and must be string
          if (Array.get(types, i) != STRING && Objects.equals(type, DOUBLE)
              || type.equals(STRING)) {
            Array.set(types, i, type);
          }
        }
        i++;
      }
    }
    return Arrays.stream(types).map((type) -> Objects.requireNonNullElse(type, STRING)).toList();
  }

  public Schema createSchemaFromTypes(List<String> types, String[] header) {
    String schemaJson =
        "{\"namespace\": \"org.molgenis.armadillo\"," // Not used in Parquet, can put anything
            + "\"type\": \"record\"," // Must be set as record
            + "\"name\": \"armadillo\"," // Not used in Parquet, can put anything
            + "\"fields\": ";
    ArrayList<String> fields = new ArrayList<>();
    AtomicInteger emptyCols = new AtomicInteger(1);
    final int[] colNumber = {0};
    Arrays.stream(header)
        .forEach(
            (headerValue) -> {
              // replace BOM if present
              headerValue = headerValue.replace("\uFEFF", "");
              if (Objects.equals(headerValue, "")) {
                headerValue = "col" + emptyCols;
                emptyCols.getAndIncrement();
              }
              this.setHeaderColname(headerValue, colNumber[0]);
              String fieldTemplate = "{\"name\" : \"%s\", \"type\": [\"%s\", \"null\"]}";
              fields.add(String.format(fieldTemplate, headerValue, types.get(colNumber[0])));
              colNumber[0]++;
            });
    Schema.Parser parser = new Schema.Parser().setValidate(true);
    return parser.parse(schemaJson.concat(fields.toString()).concat("}"));
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void writeParquet(String savePath) throws IOException, CsvValidationException {
    CSVReader reader = getReader();
    // skip header
    reader.readNext();
    // for each line create record and write
    Path outputFilePath = Paths.get(savePath);
    LocalOutputFile fileToWrite = new LocalOutputFile(outputFilePath);
    ParquetWriter<GenericData.Record> writer =
        AvroParquetWriter.<GenericData.Record>builder(fileToWrite)
            .withSchema(schema)
            .withConf(new Configuration())
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .build();
    String[] line;
    GenericData.Record record = new GenericData.Record(schema);
    while ((line = reader.readNext()) != null) {
      int i = 0;
      for (String value : line) {
        if (value.isEmpty()) {
          record.put(header[i], null);
        } else if (Objects.equals(types.get(i), DOUBLE)) {
          Double d = Double.parseDouble(value);
          record.put(header[i], d);
        } else {
          record.put(header[i], value);
        }
        i++;
      }
      try {
        writer.write(record);
      } catch (Exception e) {
        // TODO: do something here
        System.out.println(e.getMessage());
      }
    }
    writer.close();
  }
}
