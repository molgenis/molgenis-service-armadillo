package org.molgenis.connection.flower.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArmadilloDataFileTest {

  @Test
  void getProjectName_returnsThirdLastComponent() {
    ArmadilloDataFile file = new ArmadilloDataFile("project/folder/file.parquet");
    assertEquals("project", file.getProjectName());
  }

  @Test
  void getFolderName_returnsSecondLastComponent() {
    ArmadilloDataFile file = new ArmadilloDataFile("project/folder/file.parquet");
    assertEquals("folder", file.getFolderName());
  }

  @Test
  void getFileName_returnsLastComponent() {
    ArmadilloDataFile file = new ArmadilloDataFile("project/folder/file.parquet");
    assertEquals("file.parquet", file.getFileName());
  }

  @Test
  void longPath_onlyLastThreeComponentsAreUsed() {
    ArmadilloDataFile file = new ArmadilloDataFile("/tmp/data/more/project/folder/file.parquet");

    assertEquals("project", file.getProjectName());
    assertEquals("folder", file.getFolderName());
    assertEquals("file.parquet", file.getFileName());
    assertEquals("project/folder/file.parquet", file.getArmadilloFilePath());
  }

  @Test
  void fileNameWithDotsAndDashes_isPreserved() {
    ArmadilloDataFile file = new ArmadilloDataFile("my-project/my_folder/data.v1.2-final.rds");
    assertEquals("data.v1.2-final.rds", file.getFileName());
    assertEquals("my-project/my_folder/data.v1.2-final.rds", file.getArmadilloFilePath());
  }

  @Test
  void pathWithTwoComponents_throwsOnProjectName() {
    ArmadilloDataFile file = new ArmadilloDataFile("folder/file.parquet");
    assertThrows(ArrayIndexOutOfBoundsException.class, file::getProjectName);
  }

  @Test
  void pathWithTwoComponents_throwsOnArmadilloFilePath() {
    ArmadilloDataFile file = new ArmadilloDataFile("folder/file.parquet");
    assertThrows(ArrayIndexOutOfBoundsException.class, file::getArmadilloFilePath);
  }

  @Test
  void pathWithSingleComponent_throwsOnFolderName() {
    ArmadilloDataFile file = new ArmadilloDataFile("file.parquet");
    assertThrows(ArrayIndexOutOfBoundsException.class, file::getFolderName);
  }

  @Test
  void pathWithSingleComponent_stillReturnsFileName() {
    ArmadilloDataFile file = new ArmadilloDataFile("file.parquet");
    assertEquals("file.parquet", file.getFileName());
  }

  @Test
  void getSize_returnsSizeOfExistingFile(@TempDir Path tempDir) throws IOException {
    Path dir = Files.createDirectories(tempDir.resolve("project").resolve("folder"));
    Path dataFile = dir.resolve("file.txt");
    byte[] content = "hello armadillo".getBytes(StandardCharsets.UTF_8);
    Files.write(dataFile, content);

    ArmadilloDataFile file = new ArmadilloDataFile(dataFile.toString());

    assertEquals(content.length, file.getSize());
  }

  @Test
  void getSize_returnsZeroForEmptyFile(@TempDir Path tempDir) throws IOException {
    Path dir = Files.createDirectories(tempDir.resolve("project").resolve("folder"));
    Path dataFile = Files.createFile(dir.resolve("empty.txt"));

    ArmadilloDataFile file = new ArmadilloDataFile(dataFile.toString());

    assertEquals(0L, file.getSize());
  }

  @Test
  void getSize_returnsForFile(@TempDir Path tempDir) throws IOException {
    Path dir = Files.createDirectories(tempDir.resolve("project").resolve("folder"));
    Path dataFile = Files.createFile(dir.resolve("file.txt"));
    List<String> lines = Arrays.asList("1", "2", "3");
    Files.write(dataFile, lines);

    ArmadilloDataFile file = new ArmadilloDataFile(dataFile.toString());

    assertEquals(6L, file.getSize());
  }

  @Test
  void getSize_throwsForNonExistingFile(@TempDir Path tempDir) {
    Path missing = tempDir.resolve("project").resolve("folder").resolve("missing.txt");
    ArmadilloDataFile file = new ArmadilloDataFile(missing.toString());

    assertThrows(IllegalArgumentException.class, file::getSize);
  }

  @Test
  void getArmadilloFilePath_worksForRealTempFile(@TempDir Path tempDir) throws IOException {
    Path dir = Files.createDirectories(tempDir.resolve("proj").resolve("fold"));
    Path dataFile = Files.createFile(dir.resolve("data.parquet"));

    ArmadilloDataFile file = new ArmadilloDataFile(dataFile.toString());

    assertEquals("proj/fold/data.parquet", file.getArmadilloFilePath());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "project/folder/file.parquet",
        "/project/folder/file.parquet",
        "project/folder/file.parquet/"
      })
  void getArmadilloFilePath(String filePath) {
    ArmadilloDataFile file = new ArmadilloDataFile(filePath);
    assertEquals("project/folder/file.parquet", file.getArmadilloFilePath());
  }
}
