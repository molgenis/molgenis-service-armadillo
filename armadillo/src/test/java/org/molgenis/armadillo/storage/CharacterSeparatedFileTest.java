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
    assertEquals(',', csf.getSeparator());
    assertArrayEquals(new String[] {"name", "age"}, csf.getHeader());
    assertEquals(
        List.of(CharacterSeparatedFile.STRING, CharacterSeparatedFile.DOUBLE), csf.getTypes());
    assertNotNull(csf.getSchema());
  }

  @Test
  void testErrorThrownWhenSchemaCannotBeCreated() throws IOException, CsvValidationException {
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
  void testGetSeparatorFromPipe() throws IOException, CsvValidationException {
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
  void testGetTypeOfCell() throws CsvValidationException, IOException {
    assertEquals(CharacterSeparatedFile.DOUBLE, CharacterSeparatedFile.getTypeOfCell("123.45"));
    assertEquals(CharacterSeparatedFile.STRING, CharacterSeparatedFile.getTypeOfCell("abc"));
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
