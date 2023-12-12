package org.molgenis.armadillo.storage;

import static org.molgenis.armadillo.storage.ArmadilloStorageService.LINK_FILE;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minidev.json.JSONObject;
import org.molgenis.armadillo.exceptions.StorageException;

public class ArmadilloLinkFile {

  private final String SOURCE_OBJECT = "sourceObject";

  private final String SOURCE_PROJECT = "sourceProject";

  private final String VARIABLES = "variables";
  private final String sourceProject;
  private final String sourceObject;
  private final String variables;
  private final String linkObject;
  private final String project;

  public String getSourceProject() {
    return this.sourceProject;
  }
  ;

  public String getSourceObject() {
    return this.sourceObject;
  }
  ;

  public String getVariables() {
    return this.variables;
  }
  ;

  public String getLinkObject() {
    return this.linkObject;
  }
  ;

  public String toString() {
    return buildJson().toString();
  }

  public JSONObject buildJson() {
    JSONObject json = new JSONObject();
    json.put(SOURCE_OBJECT, sourceObject);
    json.put(SOURCE_PROJECT, sourceProject);
    json.put(VARIABLES, variables);
    return json;
  }

  public InputStream toStream() {
    return new ByteArrayInputStream(toString().getBytes());
  }

  public String getExtension() {
    return LINK_FILE;
  }

  public String getFileName() {
    return getLinkObject() + LINK_FILE;
  }

  public String getProject() {
    return this.project;
  }

  public JsonObject loadFromStream(InputStream inputStream) {
    return JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
  }

  public ArmadilloLinkFile(
      String sourceProject,
      String sourceObject,
      String variables,
      String linkObject,
      String project) {
    this.linkObject = linkObject;
    this.sourceProject = sourceProject;
    this.sourceObject = sourceObject;
    this.variables = variables;
    this.project = project;
  }

  public ArmadilloLinkFile(InputStream armadilloLinkStream, String linkObject, String linkProject)
      throws Exception {
    this.linkObject = linkObject;
    this.project = linkProject;

    try {
      JsonObject json = loadFromStream(armadilloLinkStream);
      this.sourceObject = json.get(SOURCE_OBJECT).getAsString();
      try {
        this.sourceProject = json.get(SOURCE_PROJECT).getAsString();
        try {
          this.variables = json.get(VARIABLES).getAsString();
        } catch (Exception e) {
          // FIXME: throw right exception
          throw new Exception(e);
        }
      } catch (Exception e) {
        // FIXME: throw right exception
        throw new Exception(e);
      }
    } catch (StorageException e) {
      // FIXME: throw right exception
      // throw new StorageException(String.format("Could not load %s/%s", linkProject, linkObject));
      throw new StorageException(e);
    } catch (Exception e) {
      // FIXME: throw right exception
      throw new Exception(e);
    }
  }
}
