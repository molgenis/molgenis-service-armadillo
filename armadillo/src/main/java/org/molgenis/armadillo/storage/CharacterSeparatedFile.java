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
import org.apache.avro.SchemaParseException;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.LocalOutputFile;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.springframework.web.multipart.MultipartFile;

public class CharacterSeparatedFile {
  public static final String STRING = "string";
  public static final String DOUBLE = "double";
  public static final String BOOLEAN = "boolean";
  public static final String INT = "int";
  MultipartFile file;
  private char separator = ',';
  Schema schema;
  String[] header;
  List<String> datatypes;
  int numberOfRowsToDetermineTypeBy = 100;

  public CharacterSeparatedFile(MultipartFile file) throws IOException, CsvValidationException {
    this.file = file;
    // test if , is real headerSeparator
    CSVReader reader = this.getReader();
    this.setHeader(reader.readNext());
    char headerSeparator = getSeparatorFromHeader(header, file);
    this.setHeaderSeparator(headerSeparator);
    // if , is not the headerSeparator, change it
    if (headerSeparator != ',') {
      reader = this.getReader();
      this.setHeader(reader.readNext());
    }
    this.datatypes = this.getTypesFromData(reader);
    this.setSchema(this.createSchemaFromTypes(this.datatypes, this.header));
  }

  public char getHeaderSeparator() {
    return separator;
  }

  public String[] getHeader() {
    return header;
  }

  public List<String> getTypes() {
    return datatypes;
  }

  public Schema getSchema() {
    return schema;
  }

  public void setNumberOfRowsToDetermineTypeBy(int numberOfRowsToDetermineTypeBy) {
    this.numberOfRowsToDetermineTypeBy = numberOfRowsToDetermineTypeBy;
  }

  public void setHeaderSeparator(char separator) {
    this.separator = separator;
  }

  public void setHeader(String[] header) {
    this.header = header;
  }

  public void setHeaderColname(String colname, int index) {
    this.header[index] = colname;
  }

  static char getSeparatorFromHeader(String[] header, MultipartFile file)
      throws CsvValidationException {
    if (header.length == 1) {
      String headerLine = header[0];
      if (headerLine.split(";").length > 1) {
        return ';';
      } else if (headerLine.split("\t").length > 1) {
        return '\t';
      } else if (headerLine.split("\\|").length > 1) {
        return '|';
      } else {
        // separator must be , ; \t or |
        throw new CsvValidationException(
            String.format(
                "Unsupported separator in file [%s] with header: [%s]",
                file.getOriginalFilename(), headerLine));
      }
    } else {
      return ',';
    }
  }

  private CSVReader getReader() throws IOException {
    CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
    return new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
        .withCSVParser(parser)
        .build();
  }

  static String getTypeOfCell(String cell) {
    try {
      double d = Double.parseDouble(cell);
      if ((d == Math.floor(d)) && !Double.isInfinite(d) && !cell.contains(".")) {
        return CharacterSeparatedFile.INT;
      } else {
        return CharacterSeparatedFile.DOUBLE;
      }
    } catch (NumberFormatException e) {
      String upperCaseCell = cell.toUpperCase();
      if (upperCaseCell.equals("TRUE")
          || upperCaseCell.equals("T")
          || upperCaseCell.equals("F")
          || upperCaseCell.equals("FALSE")) {
        return CharacterSeparatedFile.BOOLEAN;
      } else {
        return CharacterSeparatedFile.STRING;
      }
    }
  }

  static String getTypeToSet(String type, String currentType) {
    if ( // bool only when no other type found in column
    (!Objects.equals(currentType, CharacterSeparatedFile.STRING)
            && !Objects.equals(currentType, CharacterSeparatedFile.DOUBLE)
            && !Objects.equals(currentType, CharacterSeparatedFile.INT)
            && Objects.equals(type, CharacterSeparatedFile.BOOLEAN))
        ||
        // int only when no other type found in column
        (!Objects.equals(currentType, CharacterSeparatedFile.STRING)
            && !Objects.equals(currentType, CharacterSeparatedFile.DOUBLE)
            && !Objects.equals(currentType, CharacterSeparatedFile.BOOLEAN)
            && Objects.equals(type, CharacterSeparatedFile.INT))
        ||
        // double only when no string/bool found in column (int can become double)
        (!Objects.equals(currentType, CharacterSeparatedFile.STRING)
            && !Objects.equals(currentType, CharacterSeparatedFile.BOOLEAN)
            && Objects.equals(type, CharacterSeparatedFile.DOUBLE))
        // can always become string
        || type.equals(CharacterSeparatedFile.STRING)) {
      return type;
    } else if (currentType.equals(CharacterSeparatedFile.INT)
            && type.equals(CharacterSeparatedFile.DOUBLE)
        || currentType.equals(CharacterSeparatedFile.DOUBLE)
            && type.equals(CharacterSeparatedFile.INT)) {
      return CharacterSeparatedFile.DOUBLE;
    } else {
      return CharacterSeparatedFile.STRING;
    }
  }

  private List<String> getTypesFromData(CSVReader reader)
      throws IOException, CsvValidationException {
    String[] line;
    String[] types = new String[this.header.length];
    while ((line = reader.readNext()) != null
        && reader.getLinesRead() < this.numberOfRowsToDetermineTypeBy) {
      int i = 0;
      for (String value : line) {
        // determine type
        // if value is NA, it's not a string by definition
        if (!value.isEmpty() && !value.equals("NA")) {
          String typeOfCell = getTypeOfCell(value);
          String currentType = (String) Array.get(types, i);
          String type = getTypeToSet(typeOfCell, currentType);
          Array.set(types, i, type);
        }
        i++;
      }
    }
    return Arrays.stream(types).map(type -> Objects.requireNonNullElse(type, STRING)).toList();
  }

  private Schema createSchemaFromTypes(List<String> types, String[] header) {
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
            headerValue -> {
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
    try {
      return parser.parse(schemaJson.concat(fields.toString()).concat("}"));
    } catch (SchemaParseException e) {
      throw new FileProcessingException(
          String.format(
              "Cannot create CSV file, schema cannot be created from header because: [%s]",
              e.getMessage()));
    }
  }

  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  public void writeParquet(String savePath) throws IOException, CsvValidationException {
    CSVReader reader = getReader();
    // skip header
    reader.readNext();
    // for each line create dataRecord and write
    Path outputFilePath = Paths.get(savePath);
    LocalOutputFile fileToWrite = new LocalOutputFile(outputFilePath);
    ParquetWriter<GenericData.Record> writer =
        AvroParquetWriter.<GenericData.Record>builder(fileToWrite)
            .withSchema(schema)
            .withConf(new Configuration())
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .build();
    String[] line;
    GenericData.Record dataRecord = new GenericData.Record(schema);
    while ((line = reader.readNext()) != null) {
      int i = 0;
      for (String value : line) {
        if (value.isEmpty() || value.equals("NA")) {
          dataRecord.put(header[i], null);
        } else if (Objects.equals(datatypes.get(i), DOUBLE)) {
          Double d = Double.parseDouble(value);
          dataRecord.put(header[i], d);
        } else {
          dataRecord.put(header[i], value);
        }
        i++;
      }
      try {
        writer.write(dataRecord);
      } catch (Exception e) {
        throw new WriteAbortedException(
            String.format("Cannot write parquet file because: [%s]", e.getMessage()), e);
      }
    }
    writer.close();
  }
}
