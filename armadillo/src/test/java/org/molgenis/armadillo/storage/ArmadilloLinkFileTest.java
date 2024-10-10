package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.armadillo.storage.ArmadilloLinkFile.isValidLinkObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class ArmadilloLinkFileTest {
  String srcObj = "folder/src-obj";
  String srcProject = "src-project";
  String vars = "var1,var2,var3";
  String testData =
      "{\"sourceObject\":\"folder/src-obj\",\"sourceProject\":\"src-project\",\"variables\":\"var1,var2,var3\"}";
  String linkObj = "folder/link-obj";

  String linkProject = "view-project";
  ArmadilloLinkFile alf = new ArmadilloLinkFile(srcProject, srcObj, vars, linkObj, linkProject);

  @Test
  public void testBuildJson() {
    JsonObject actual = alf.buildJson();
    assertEquals(srcObj, actual.get("sourceObject").getAsString());
    assertEquals(srcProject, actual.get("sourceProject").getAsString());
    assertEquals(vars, actual.get("variables").getAsString());
  }

  @Test
  public void testToString() {
    String actual = alf.toString();
    assertEquals(testData, actual);
  }

  @Test
  public void testGetFileName() {
    String actual = alf.getFileName();
    assertEquals("folder/link-obj.alf", actual);
  }

  @Test
  public void testGetNumberOfVariables() {
    Integer actual = alf.getNumberOfVariables();
    assertEquals(3, actual);
  }

  @Test
  public void testLoadFromStream() {
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    JsonObject actual = alf.loadFromStream(inputStream);
    assertEquals(testData, actual.toString());
  }

  @Test
  public void testLoadLinkFileFromStream() {
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    ArmadilloLinkFile alfFromStream = new ArmadilloLinkFile(inputStream, linkObj, linkProject);
    JsonObject jsonFromStream = alfFromStream.buildJson();
    assertEquals(srcObj, jsonFromStream.get("sourceObject").getAsString());
    assertEquals(srcProject, jsonFromStream.get("sourceProject").getAsString());
    assertEquals(vars, jsonFromStream.get("variables").getAsString());
  }

  @Test
  public void testLoadLinkFileFromStreamInvalidJson() {
    String testData =
        "\"sourceObject\":\"folder/src-obj\",\"sourceProject\":\"src-project\",\"variables\":\"var1,var2,var3\"}";
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    try {
      new ArmadilloLinkFile(inputStream, linkProject, linkObj);
    } catch (JsonParseException e) {
      String message = format("Cannot load [%s/%s] because JSON is invalid", linkProject, linkObj);
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testLoadLinkFileFromStreamInvalidObj() {
    String testData =
        "{\"sourceObj\":\"folder/src-obj\",\"sourceProject\":\"src-project\",\"variables\":\"var1,var2,var3\"}";
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    try {
      new ArmadilloLinkFile(inputStream, linkProject, linkObj);
    } catch (NullPointerException e) {
      String message = format("Source object is missing from [%s/%s]", linkProject, linkObj);
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testLoadLinkFileFromStreamInvalidProject() {
    String testData =
        "{\"sourceObject\":\"folder/src-obj\",\"sourceProj\":\"src-project\",\"variables\":\"var1,var2,var3\"}";
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    try {
      new ArmadilloLinkFile(inputStream, linkProject, linkObj);
    } catch (NullPointerException e) {
      String message = format("Source project is missing from [%s/%s]", linkProject, linkObj);
      assertEquals(message, e.getMessage());
    }
  }

  @Test
  public void testIsValidLinkObjectTooManySlashes() {
    String tooManySlashes = "/this/is/too/many";
    assertFalse(isValidLinkObject(tooManySlashes));
  }

  @Test
  public void testIsValidLinkObjectEndsWithSlash() {
    String endsWithSlash = "endswith/";
    assertFalse(isValidLinkObject(endsWithSlash));
  }

  @Test
  public void testIsValidLinkObjectNotEnoughSlashes() {
    String noSlash = "no slash";
    assertFalse(isValidLinkObject(noSlash));
  }

  @Test
  public void testIsValidLinkObjectSucceeds() {
    String object = "folder/file";
    assertTrue(isValidLinkObject(object));
  }

  @Test
  public void testLoadLinkFileFromStreamInvalidVariables() {
    String testData =
        "{\"sourceObject\":\"folder/src-obj\",\"sourceProject\":\"src-project\",\"variabls\":\"var1,var2,var3\"}";
    InputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    try {
      new ArmadilloLinkFile(inputStream, linkProject, linkObj);
    } catch (NullPointerException e) {
      String message = format("Variables are not defined on [%s/%s]", linkProject, linkObj);
      assertEquals(message, e.getMessage());
    }
  }
}
