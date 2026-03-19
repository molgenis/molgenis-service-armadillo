package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.opencsv.exceptions.CsvValidationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.springframework.web.multipart.MultipartFile;

class CharacterSeparatedFileTest {

  MultipartFile mockFile;

  @BeforeEach
  void setup() {
    mockFile = mock(MultipartFile.class);
  }

  @Test
  void testConstructorWithCommaSeparator() throws IOException, CsvValidationException {
    String csvData = "name,age\nJohn,30\nJane,25\n";
    Mockito.when(mockFile.getInputStream())
        .thenReturn(new ByteArrayInputStream(csvData.getBytes()));
    Mockito.when(mockFile.getOriginalFilename()).thenReturn("test.csv");

    CharacterSeparatedFile csf = new CharacterSeparatedFile(mockFile);

    assertNotNull(csf);
    assertEquals(',', csf.getHeaderSeparator());
    assertArrayEquals(new String[] {"name", "age"}, csf.getHeader());
    assertEquals(
        List.of(CharacterSeparatedFile.STRING, CharacterSeparatedFile.INT), csf.getTypes());
    assertNotNull(csf.getSchema());
  }

  @Test
  void testErrorThrownWhenSchemaCannotBeCreated() throws IOException {
    String csvData = "name of person,age\nJohn,30\nJane,25\n";
    Mockito.when(mockFile.getInputStream())
        .thenReturn(new ByteArrayInputStream(csvData.getBytes()));
    Mockito.when(mockFile.getOriginalFilename()).thenReturn("test.csv");

    assertThrows(FileProcessingException.class, () -> new CharacterSeparatedFile(mockFile));
  }

  @Test
  void testGetSeparatorFromSemicolon() throws CsvValidationException {
    char sep = CharacterSeparatedFile.getSeparatorFromHeader(new String[] {"name;age"}, mockFile);
    assertEquals(';', sep);
  }

  @Test
  void testGetSeparatorFromTab() throws CsvValidationException {
    char sep = CharacterSeparatedFile.getSeparatorFromHeader(new String[] {"name\tage"}, mockFile);
    assertEquals('\t', sep);
  }

  @Test
  void testGetSeparatorFromPipe() throws CsvValidationException {
    char sep = CharacterSeparatedFile.getSeparatorFromHeader(new String[] {"name|age"}, mockFile);
    assertEquals('|', sep);
  }

  @Test
  void testImplySeparatorThrowsOnUnsupportedSeparator() {
    String csvData = "name#age\nJohn#30\n";
    Mockito.when(mockFile.getOriginalFilename()).thenReturn("bad.csv");

    Exception exception =
        assertThrows(
            CsvValidationException.class,
            () -> {
              Mockito.when(mockFile.getInputStream())
                  .thenReturn(new ByteArrayInputStream(csvData.getBytes()));
              new CharacterSeparatedFile(mockFile);
            });

    assertTrue(exception.getMessage().contains("Unsupported separator"));
  }

  @Test
  void testGetTypeOfCellDouble() {
    assertEquals(CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.getTypeOfCell("123.45"));
    assertEquals(CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.getTypeOfCell("1.0"));
  }

  @Test
  void testGetTypeOfCellInt() {
    assertEquals(CharacterSeparatedFile.INT, CharacterSeparatedFile.getTypeOfCell("1"));
  }

  @Test
  void testGetTypeOfCellBool() {
    assertEquals(CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.getTypeOfCell("T"));
    assertEquals(CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.getTypeOfCell("f"));
    assertEquals(CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.getTypeOfCell("True"));
    assertEquals(CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.getTypeOfCell("FALSE"));
  }

  @Test
  void testGetTypeOfCellString() {
    assertEquals(CharacterSeparatedFile.STRING, CharacterSeparatedFile.getTypeOfCell("abc"));
  }

  @Test
  void testGetTypeToSetBoolean() {
    assertEquals(
        CharacterSeparatedFile.BOOLEAN,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.BOOLEAN));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.STRING, CharacterSeparatedFile.BOOLEAN));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.INT, CharacterSeparatedFile.BOOLEAN));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.BOOLEAN));
  }

  @Test
  void testGetTypeToSetInt() {
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.INT));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.STRING, CharacterSeparatedFile.INT));
    assertEquals(
        CharacterSeparatedFile.INT,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.INT, CharacterSeparatedFile.INT));
    assertEquals(
        CharacterSeparatedFile.DOUBLE,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.INT));
  }

  @Test
  void testGetTypeToSetString() {
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.STRING));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.STRING, CharacterSeparatedFile.STRING));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.INT, CharacterSeparatedFile.STRING));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.STRING));
  }

  @Test
  void testGetTypeToSetDouble() {
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.BOOLEAN, CharacterSeparatedFile.DOUBLE));
    assertEquals(
        CharacterSeparatedFile.STRING,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.STRING, CharacterSeparatedFile.DOUBLE));
    assertEquals(
        CharacterSeparatedFile.DOUBLE,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.INT, CharacterSeparatedFile.DOUBLE));
    assertEquals(
        CharacterSeparatedFile.DOUBLE,
        CharacterSeparatedFile.getTypeToSet(
            CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.DOUBLE));
  }

  @Test
  void testGetBooleanValueTrue() {
    assertEquals(Boolean.TRUE, CharacterSeparatedFile.getBooleanValue("T"));
    assertEquals(Boolean.TRUE, CharacterSeparatedFile.getBooleanValue("True"));
  }

  @Test
  void testGetBooleanValueFalse() {
    assertEquals(Boolean.FALSE, CharacterSeparatedFile.getBooleanValue("F"));
    assertEquals(Boolean.FALSE, CharacterSeparatedFile.getBooleanValue("falSE"));
  }

  @Test
  void testGetCorrectlyTypedDataRecordNull() {
    assertNull(
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("NA", CharacterSeparatedFile.STRING));
    assertNull(
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("NA", CharacterSeparatedFile.INT));
    assertNull(
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("NA", CharacterSeparatedFile.BOOLEAN));
    assertNull(
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("NA", CharacterSeparatedFile.DOUBLE));
  }

  @Test
  void testGetCorrectlyTypedDataRecordString() {
    assertEquals(
        "Very Random String",
        CharacterSeparatedFile.getCorrectlyTypedDataRecord(
            "Very Random String", CharacterSeparatedFile.STRING));
    assertEquals(
        "true",
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("true", CharacterSeparatedFile.STRING));
    assertEquals(
        "1.09",
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("1.09", CharacterSeparatedFile.STRING));
    assertEquals(
        "1",
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("1", CharacterSeparatedFile.STRING));
  }

  @Test
  void testGetCorrectlyTypedDataRecordInt() {
    assertEquals(
        7, CharacterSeparatedFile.getCorrectlyTypedDataRecord("7", CharacterSeparatedFile.INT));
  }

  @Test
  void testGetCorrectlyTypedDataRecordDouble() {
    assertEquals(
        1.092,
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("1.092", CharacterSeparatedFile.DOUBLE));
    assertEquals(
        7.0,
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("7", CharacterSeparatedFile.DOUBLE));
  }

  @Test
  void testGetCorrectlyTypedDataRecordBool() {
    assertEquals(
        true,
        CharacterSeparatedFile.getCorrectlyTypedDataRecord("tRuE", CharacterSeparatedFile.BOOLEAN));
    assertEquals(
        false,
        CharacterSeparatedFile.getCorrectlyTypedDataRecord(
            "FALSE", CharacterSeparatedFile.BOOLEAN));
  }

  @Test
  void testWriteParquetCreatesFile() throws IOException, CsvValidationException {
    String csvData = "name,age\nAlice,35\nBob,28\n";
    Path tempDirWithPrefix = Files.createTempDirectory("temp");
    String savePath = tempDirWithPrefix.toString() + "/test.parquet";

    Mockito.when(mockFile.getInputStream())
        .thenReturn(new ByteArrayInputStream(csvData.getBytes()));
    Mockito.when(mockFile.getOriginalFilename()).thenReturn("test.csv");

    CharacterSeparatedFile csf = new CharacterSeparatedFile(mockFile);
    csf.writeParquet(savePath);

    File f = new File(savePath);
    assertTrue(f.exists());
    FileUtils.deleteDirectory(tempDirWithPrefix.toFile());
  }
}
