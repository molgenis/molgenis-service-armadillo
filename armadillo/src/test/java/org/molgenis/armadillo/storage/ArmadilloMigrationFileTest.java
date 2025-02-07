package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

class ArmadilloMigrationFileTest {

  private static ArmadilloMigrationFile armadilloMigrationFile;
  private static Path tempDirectory;
  private static String MIGRATION_FILE_PATH = "/migration-status.amf";

  @BeforeEach
  void setUp() throws IOException {
    tempDirectory = Files.createTempDirectory("root");
    armadilloMigrationFile = new ArmadilloMigrationFile(tempDirectory.toString(), "");
  }

  @AfterEach
  void tearDown() throws IOException {
    FileUtils.deleteDirectory(tempDirectory.toFile());
  }

  @Test
  void testConstructor() {
    // Verify the path was correctly set in the constructor
    assertNotNull(armadilloMigrationFile.migrationFilePath);
    assertEquals(
        Paths.get(tempDirectory + MIGRATION_FILE_PATH).toAbsolutePath().normalize(),
        armadilloMigrationFile.migrationFilePath);
  }

  @Test
  void testGetMigrationStatusSuccess() throws IOException {
    // Mock file reading
    String testData =
        "Successfully migrated workspace [cohort:workspace-name.RData] from [user-old] to [user-new]\n";
    Path tempFile = Files.createFile(Path.of(tempDirectory + MIGRATION_FILE_PATH));
    Files.write(tempFile, testData.getBytes(), StandardOpenOption.CREATE);

    ArrayList<HashMap<String, String>> status = armadilloMigrationFile.getMigrationStatus();

    assertEquals(1, status.size());
    HashMap<String, String> migration = status.get(0);
    assertEquals("cohort:workspace-name.RData", migration.get("workspace"));
    assertEquals("user-old", migration.get("oldUserFolder"));
    assertEquals("user-new", migration.get("newUserFolder"));
    assertEquals("success", migration.get("status"));

    Files.delete(tempFile);
  }

  @Test
  void testGetMigrationStatusFailure() throws IOException {
    String testData =
        "Cannot migrate workspace [cohort:workspace-name.RData] from [user-old] to [user-new], because [error]. Workspace needs to be moved manually.\n";
    Path tempFile = Files.createFile(Path.of(tempDirectory + MIGRATION_FILE_PATH));
    Files.write(tempFile, testData.getBytes(), StandardOpenOption.CREATE);

    ArrayList<HashMap<String, String>> status = armadilloMigrationFile.getMigrationStatus();

    assertEquals(1, status.size());
    HashMap<String, String> migration = status.get(0);
    assertEquals("cohort:workspace-name.RData", migration.get("workspace"));
    assertEquals("user-old", migration.get("oldUserFolder"));
    assertEquals("user-new", migration.get("newUserFolder"));
    assertEquals("failure", migration.get("status"));
    assertEquals("error", migration.get("errorMessage"));

    Files.delete(tempFile);
  }

  @Test
  void testGetMigrationSuccessMessage() {
    String message =
        armadilloMigrationFile.getMigrationSuccessMessage("workspace-name", "user-old", "user-new");
    String expectedMessage =
        "Successfully migrated workspace [workspace-name] from [user-old] to [user-new]\n";
    assertEquals(expectedMessage, message);
  }

  @Test
  void testGetMigrationFailureMessage() {
    String message =
        armadilloMigrationFile.getMigrationFailureMessage(
            "workspace-name", "user-old", "user-new", "error");
    String expectedMessage =
        "Cannot migrate workspace [workspace-name] from [user-old] to [user-new], because [error]. Workspace needs to be moved manually.\n";
    assertEquals(expectedMessage, message);
  }

  @Test
  void testAddLineToExistingFile() throws IOException {
    Path tempFile = Files.createFile(Path.of(tempDirectory + MIGRATION_FILE_PATH));
    Files.write(tempFile, "Existing line\n".getBytes(), StandardOpenOption.CREATE);
    String lineToAdd = "New line to add\n";

    armadilloMigrationFile.addLine(lineToAdd);

    // Verify the file content now contains both lines
    String content = Files.readString(tempFile);
    assertTrue(content.contains("Existing line"));
    assertTrue(content.contains("New line to add"));

    Files.delete(tempFile);
  }

  @Test
  void testAddLineToNewFile() throws IOException {
    String lineToAdd = "New line to add\n";

    armadilloMigrationFile.addLine(lineToAdd);

    // Verify the file was created and contains the correct content
    String content = Files.readString(Path.of(tempDirectory + MIGRATION_FILE_PATH));
    assertTrue(content.contains("New line to add"));
  }
}
