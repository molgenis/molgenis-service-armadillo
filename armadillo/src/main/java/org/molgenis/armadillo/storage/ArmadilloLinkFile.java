package org.molgenis.armadillo.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.minidev.json.JSONObject;

public class ArmadilloLinkFile {

  private final String extension = ".alf";
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
    return toJson().toString();
  }

  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("sourceObject", sourceObject);
    json.put("sourceProject", sourceProject);
    json.put("variables", variables);
    return json;
  }

  public InputStream toStream() {
    return new ByteArrayInputStream(toString().getBytes());
  }

  public String getExtension() {
    return this.extension;
  }

  public String getFileName() {
    return getLinkObject() + extension;
  }

  public String getProject() {
    return this.project;
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
}
