package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.LINK_FILE;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    JsonObject json;
    try {
      json = loadFromStream(armadilloLinkStream);
    } catch (JsonParseException e) {
      throw new JsonParseException(
          format("Cannot load [%s/%s] because JSON is invalid", project, linkObject));
    } catch (Exception e) {
      throw new StorageException(
          format("Cannot load [%s/%s] for unknown reason", project, linkObject));
    }
    try {
      this.sourceObject = json.get(SOURCE_OBJECT).getAsString();
    } catch (NullPointerException e) {
      throw new NullPointerException(
          format("Source object is missing from [%s/%s]", project, linkObject));
    }
    try {
      this.sourceProject = json.get(SOURCE_PROJECT).getAsString();
    } catch (NullPointerException e) {
      throw new NullPointerException(
          format("Source project is missing from [%s/%s]", project, linkObject));
    }
    try {
      this.variables = json.get(VARIABLES).getAsString();
    } catch (NullPointerException e) {
      throw new NullPointerException(
          format("Variables are not defined on [%s/%s]", project, linkObject));
    }
  }

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

  public JsonObject buildJson() {
    JsonObject json = new JsonObject();
    json.addProperty(SOURCE_OBJECT, sourceObject);
    json.addProperty(SOURCE_PROJECT, sourceProject);
    json.addProperty(VARIABLES, variables);
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
}
