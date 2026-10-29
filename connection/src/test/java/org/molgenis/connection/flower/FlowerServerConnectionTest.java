package org.molgenis.connection.flower;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.hc.core5.http.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlowerServerConnectionTest {

  @Mock FlowerContainerConnection flowerContainerConnection;

  FlowerServerConnection flowerServerConnection;

  @BeforeEach
  void setUp() {
    flowerServerConnection = new FlowerServerConnection(flowerContainerConnection);
  }

  @Test
  void eval_throwsNotImplementedException() {
    assertThrows(NotImplementedException.class, () -> flowerServerConnection.eval("1 + 1"));
    verifyNoInteractions(flowerContainerConnection);
  }

  @Test
  void readFile_throwsNotImplementedException() {
    assertThrows(
        NotImplementedException.class,
        () -> flowerServerConnection.readFile("project/folder/file.txt", in -> {}));
    verifyNoInteractions(flowerContainerConnection);
  }

  @Test
  void close_returnsFalse() {
    assertFalse(flowerServerConnection.close());
  }

  @Test
  void writeFile_pushesUrlEncodedNameStreamAndSize(@TempDir Path tempDir) throws IOException {
    byte[] content = "hello flower".getBytes(StandardCharsets.UTF_8);
    Path dataFile = createFile(tempDir, "proj", "fold", "file.txt", content);
    InputStream in = new ByteArrayInputStream(content);

    flowerServerConnection.writeFile(dataFile.toString(), in);

    verify(flowerContainerConnection).pushData("proj%2Ffold%2Ffile.txt", in, content.length);
  }

  @Test
  void writeFile_usesOnlyLastThreePathComponents(@TempDir Path tempDir) throws IOException {
    byte[] content = {1, 2, 3};
    Path dataFile = createFile(tempDir, "project", "folder", "data.parquet", content);
    InputStream in = new ByteArrayInputStream(content);

    flowerServerConnection.writeFile(dataFile.toString(), in);

    verify(flowerContainerConnection).pushData("project%2Ffolder%2Fdata.parquet", in, 3L);
  }

  @Test
  void writeFile_encodesSlashesSoFoldersCannotCollideWithUnderscores(@TempDir Path tempDir)
      throws IOException {
    byte[] content = {1};
    Path withSlash = createFile(tempDir, "proj", "data", "train.txt", content);
    Path withUnderscore = createFile(tempDir, "proj", "data_train", "txt", content);
    InputStream in1 = new ByteArrayInputStream(content);
    InputStream in2 = new ByteArrayInputStream(content);

    flowerServerConnection.writeFile(withSlash.toString(), in1);
    flowerServerConnection.writeFile(withUnderscore.toString(), in2);

    verify(flowerContainerConnection).pushData("proj%2Fdata%2Ftrain.txt", in1, 1L);
    verify(flowerContainerConnection).pushData("proj%2Fdata_train%2Ftxt", in2, 1L);
  }

  @Test
  void writeFile_encodesSpecialCharactersInFileName(@TempDir Path tempDir) throws IOException {
    byte[] content = {1, 2};
    Path dataFile = createFile(tempDir, "proj", "fold", "my file.txt", content);
    InputStream in = new ByteArrayInputStream(content);

    flowerServerConnection.writeFile(dataFile.toString(), in);

    verify(flowerContainerConnection).pushData("proj%2Ffold%2Fmy+file.txt", in, 2L);
  }

  @Test
  void writeFile_pushesZeroSizeForEmptyFile(@TempDir Path tempDir) throws IOException {
    Path dataFile = createFile(tempDir, "proj", "fold", "empty.txt", new byte[0]);
    InputStream in = new ByteArrayInputStream(new byte[0]);

    flowerServerConnection.writeFile(dataFile.toString(), in);

    verify(flowerContainerConnection).pushData("proj%2Ffold%2Fempty.txt", in, 0L);
  }

  @Test
  void writeFile_throwsForNonExistingFile_andDoesNotPush(@TempDir Path tempDir) {
    String missing = tempDir.resolve("proj").resolve("fold").resolve("missing.txt").toString();
    InputStream in = new ByteArrayInputStream(new byte[0]);

    assertThrows(
        IllegalArgumentException.class, () -> flowerServerConnection.writeFile(missing, in));

    verify(flowerContainerConnection, never()).pushData(anyString(), any(), anyLong());
  }

  @Test
  void writeFile_throwsForPathWithTooFewComponents_andDoesNotPush() {
    InputStream in = new ByteArrayInputStream(new byte[0]);

    assertThrows(
        ArrayIndexOutOfBoundsException.class,
        () -> flowerServerConnection.writeFile("folder/file.txt", in));

    verify(flowerContainerConnection, never()).pushData(anyString(), any(), eq(0L));
  }

  private static Path createFile(
      Path root, String project, String folder, String fileName, byte[] content)
      throws IOException {
    Path dir = Files.createDirectories(root.resolve(project).resolve(folder));
    return Files.write(dir.resolve(fileName), content);
  }
}
