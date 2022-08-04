package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.List;
import javax.validation.constraints.NotBlank;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("/storage")
@Validated
@PreAuthorize("hasRole('ROLE_SU')")
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

  @PostMapping("/projects")
  @ResponseStatus(NO_CONTENT)
  public void createProject(@NotBlank String project) {
    // TODO storage.createProject()
  }

  @RequestMapping(value = "/projects/{project}", method = HEAD)
  public ResponseEntity<Void> projectExists(@PathVariable String project) {
    // TODO storage.projectExists()
    var result = true;
    return result ? noContent().build() : notFound().build();
  }

  @DeleteMapping("/projects/{project}")
  @ResponseStatus(NO_CONTENT)
  public void deleteProject(@PathVariable String project) {
    // TODO 404 when project doesn't exist
    // TODO storage.deleteProject()
  }

  @GetMapping("/projects/{project}/objects")
  @ResponseStatus(OK)
  public List<String> listObjects(@PathVariable String project) {
    // TODO seperate endpoint for tables and resources?
    // TODO 404 when project doesn't exist
    var objects = storage.listTables(project);
    objects.addAll(storage.listResources(project));
    return objects;
  }

  @PostMapping("/projects/{project}/objects")
  @ResponseStatus(NO_CONTENT)
  public void uploadObject(@PathVariable String project, @RequestParam MultipartFile file) {
    // TODO 404 when project doesn't exist
    // TODO storage.writeObject()
  }

  @PostMapping("/projects/{project}/objects")
  @ResponseStatus(NO_CONTENT)
  public void copyObject(@PathVariable String project, @RequestParam("copyOf") String object) {
    // TODO 404 when project or object doesn't exist
    // TODO storage.copyObject()
  }

  @PostMapping("/projects/{project}/objects")
  @ResponseStatus(NO_CONTENT)
  public void moveObject(@PathVariable String project, @RequestParam("moveFrom") String object) {
    // TODO 404 when project or object doesn't exist
    // TODO storage.moveObject()
  }

  @RequestMapping(value = "/projects/{project}/objects/{object}", method = HEAD)
  public ResponseEntity<Void> objectExists(
      @PathVariable String project, @PathVariable String object) {
    // TODO 404 when project doesn't exist
    // TODO storage.hasObject(project)
    var result = true;
    return result ? noContent().build() : notFound().build();
  }

  @DeleteMapping("/projects/{project}/objects/{object}")
  @ResponseStatus(NO_CONTENT)
  public void deleteObject(@PathVariable String project, @PathVariable String object) {
    // TODO 404 when project or object doesn't exist
    // TODO storage.deleteObject(project)
  }

  @GetMapping("/projects/{project}/objects/{object}")
  @ResponseStatus(OK)
  public @ResponseBody byte[] getObject(@PathVariable String project, @PathVariable String object) {
    // TODO 404 when project or object doesn't exist
    // TODO storage.loadObject()
    return null;
  }
}
