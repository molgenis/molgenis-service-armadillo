package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.LINK_FILE;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

  public ArmadilloLinkFile(InputStream armadilloLinkStream, String linkObject, String linkProject) {
    this.linkObject = linkObject;
    this.project = linkProject;

    try {
      JsonObject json = loadFromStream(armadilloLinkStream);
      this.sourceObject = json.get(SOURCE_OBJECT).getAsString();
      try {
        this.sourceProject = json.get(SOURCE_PROJECT).getAsString();
        try {
          this.variables = json.get(VARIABLES).getAsString();
        } catch (NullPointerException e) {
          throw new NullPointerException(
              format("Variables are not defined on [%s/%s]", project, linkObject));
        }
      } catch (NullPointerException e) {
        throw new NullPointerException(
            format("Source project is missing from [%s/%s]", project, linkObject));
      }
    } catch (NullPointerException e) {
      throw new NullPointerException(
          format("Source object is missing from [%s/%s]", project, linkObject));
    } catch (JsonParseException e) {
      throw new JsonParseException(
          format("Cannot load [%s/%s] because JSON is invalid", project, linkObject));
    } catch (Exception e) {
      throw new StorageException(
          format("Cannot load [%s/%s] for unknown reason", project, linkObject));
    }
  }
}
