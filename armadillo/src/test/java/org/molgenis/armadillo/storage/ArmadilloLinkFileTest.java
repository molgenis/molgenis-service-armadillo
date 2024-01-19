package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class ArmadilloLinkFileTest {
  String srcObj = "folder/src-obj";
  String srcProject = "src-project";
  String vars = "var1,var2,var3";
  ArmadilloLinkFile alf =
      new ArmadilloLinkFile(srcProject, srcObj, vars, "folder/link-obj", "view-project");

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
    String expected =
        "{\"sourceObject\":\"folder/src-obj\",\"sourceProject\":\"src-project\",\"variables\":\"var1,var2,var3\"}";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetFileName() {
    String actual = alf.getFileName();
    assertEquals("folder/link-obj.alf", actual);
  }

  @Test
  public void testLoadFromStream() {
    String testString = "{\"test\":\"test\"}";
    InputStream inputStream = new ByteArrayInputStream(testString.getBytes());
    JsonObject actual = alf.loadFromStream(inputStream);
    assertEquals(testString, actual.toString());
  }
}
