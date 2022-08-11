package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.io.IOException;
import java.util.List;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo - storage API", version = "0.1.0"),
    security = {
      @SecurityRequirement(name = "JSESSIONID"),
      @SecurityRequirement(name = "http"),
      @SecurityRequirement(name = "jwt")
    })
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@SecurityScheme(name = "jwt", in = HEADER, type = APIKEY)
@RestController
@Validated
@PreAuthorize("hasRole('ROLE_SU')")
@RequestMapping("storage")
public class StorageController {

  private final ArmadilloStorageService storage;

  public StorageController(ArmadilloStorageService storage) {
    this.storage = storage;
  }

  @GetMapping("/projects")
  @ResponseStatus(OK)
  public List<String> listProjects() {
    return storage.listProjects();
  }

  @PostMapping(
      value = "/projects",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void createProject(@RequestBody ProjectRequestBody project) {
    storage.createProject(project.name());
  }

  @RequestMapping(value = "/projects/{project}", method = HEAD)
  public ResponseEntity<Void> projectExists(@PathVariable String project) {
    return storage.hasProject(project) ? noContent().build() : notFound().build();
  }

  @DeleteMapping("/projects/{project}")
  @ResponseStatus(NO_CONTENT)
  public void deleteProject(@PathVariable String project) {
    storage.deleteProject(project);
  }

  @GetMapping("/projects/{project}/objects")
  @ResponseStatus(OK)
  public List<String> listObjects(@PathVariable String project) {
    return storage.listObjects(project);
  }

  @PostMapping(
      value = "/projects/{project}/objects",
      consumes = {MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void uploadObject(@PathVariable String project, @RequestParam MultipartFile file) {
    try {
      storage.addObject(project, file.getOriginalFilename(), file.getInputStream());
    } catch (IOException e) {
      throw new FileProcessingException();
    }
  }

  @PostMapping(
      value = "/projects/{project}/objects",
      params = "copyOf",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void copyObject(
      @PathVariable String project,
      @RequestBody ObjectRequestBody newObject,
      @RequestParam("copyOf") String sourceObject) {
    storage.copyObject(project, newObject.name(), sourceObject);
  }

  @PostMapping(
      value = "/projects/{project}/objects",
      params = "movedFrom",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void moveObject(
      @PathVariable String project,
      @RequestBody ObjectRequestBody newObject,
      @RequestParam("movedFrom") String oldObject) {
    storage.moveObject(project, newObject.name(), oldObject);
  }

  @RequestMapping(value = "/projects/{project}/objects/{object}", method = HEAD)
  public ResponseEntity<Void> objectExists(
      @PathVariable String project, @PathVariable String object) {
    return storage.hasObject(project, object) ? noContent().build() : notFound().build();
  }

  @DeleteMapping("/projects/{project}/objects/{object}")
  @ResponseStatus(NO_CONTENT)
  public void deleteObject(@PathVariable String project, @PathVariable String object) {
    storage.deleteObject(project, object);
  }

  @GetMapping("/projects/{project}/objects/{object}")
  public @ResponseBody ResponseEntity<ByteArrayResource> getObject(
      @PathVariable String project, @PathVariable String object) {
    var inputStream = storage.loadObject(project, object);
    var objectParts = object.split("/");
    var fileName = objectParts[objectParts.length - 1];

    try {
      var resource = new ByteArrayResource(inputStream.readAllBytes());
      return ResponseEntity.ok()
          .header(CONTENT_DISPOSITION, "attachment; filename=" + fileName)
          .contentLength(resource.contentLength())
          .contentType(APPLICATION_OCTET_STREAM)
          .body(resource);
    } catch (IOException e) {
      throw new FileProcessingException();
    }
  }
}
