package org.molgenis.armadillo.controller;

import static org.apache.logging.log4j.util.Strings.concat;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.LINK_FILE;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import com.opencsv.exceptions.CsvValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.molgenis.armadillo.exceptions.UnknownObjectException;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.molgenis.armadillo.model.ArmadilloColumnMetaData;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.molgenis.armadillo.storage.FileInfo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "storage", description = "API to manipulate the storage")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("storage")
public class StorageController {

  private final ArmadilloStorageService storage;
  private final AuditEventPublisher auditor;

  public StorageController(ArmadilloStorageService storage, AuditEventPublisher auditor) {
    this.storage = storage;
    this.auditor = auditor;
  }

  @Operation(summary = "List objects in a project")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Objects listed",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))),
        @ApiResponse(
            responseCode = "404",
            description = "Project does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(
      value = "/projects/{project}/objects",
      produces = {APPLICATION_JSON_VALUE})
  @ResponseStatus(OK)
  public List<String> listObjects(Principal principal, @PathVariable String project) {
    return auditor.audit(
        () -> storage.listObjects(project), principal, LIST_OBJECTS, Map.of(PROJECT, project));
  }

  @Operation(summary = "Upload an object to a project")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object uploaded successfully"),
        @ApiResponse(responseCode = "404", description = "Unknown project"),
        @ApiResponse(responseCode = "409", description = "Object already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(
      value = "/projects/{project}/objects",
      consumes = {MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void uploadObject(
      Principal principal,
      @PathVariable String project,
      @RequestParam @NotEmpty String object,
      @Valid @RequestParam MultipartFile file) {
    auditor.audit(
        () -> addObject(project, object, file),
        principal,
        UPLOAD_OBJECT,
        Map.of(PROJECT, project, OBJECT, object));
  }

  @Operation(summary = "Upload a csv file to a project")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object uploaded successfully"),
        @ApiResponse(responseCode = "404", description = "Unknown project"),
        @ApiResponse(responseCode = "409", description = "Object already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(
      value = "/projects/{project}/csv",
      consumes = {MULTIPART_FORM_DATA_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void uploadCharacterSeparatedFile(
      Principal principal,
      @PathVariable String project,
      @RequestParam @NotEmpty String object,
      @RequestParam int numberOfRowsToDetermineTypeBy,
      @Valid @RequestParam MultipartFile file) {
    auditor.audit(
        () -> {
          try {
            addParquetObject(project, object, file, numberOfRowsToDetermineTypeBy);
          } catch (IOException | CsvValidationException | FileProcessingException e) {
            throw new FileProcessingException(
                String.format(
                    "Could not process file: [%s] because: [%s]",
                    file.getOriginalFilename(), e.getMessage()));
          }
        },
        principal,
        UPLOAD_OBJECT,
        Map.of(PROJECT, project, OBJECT, object));
  }

  void addObject(String project, String object, MultipartFile file) {
    try {
      storage.addObject(project, object, file.getInputStream());
    } catch (IOException e) {
      throw new FileProcessingException();
    }
  }

  private void addParquetObject(
      String project, String object, MultipartFile file, int numberOfRowsToDetermineTypeBy)
      throws CsvValidationException, IOException {
    storage.writeParquetFromCsv(project, object, file, numberOfRowsToDetermineTypeBy);
  }

  @Operation(
      summary = "Copy an object within a project",
      description =
          "The request body should contain the new object's name in full (e.g. core/nonrep.parquet)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object copied successfully"),
        @ApiResponse(responseCode = "404", description = "Unknown project or object"),
        @ApiResponse(responseCode = "409", description = "Object already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(
      value = "/projects/{project}/objects/{object}/copy",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void copyObject(
      Principal principal,
      @PathVariable String project,
      @PathVariable String object,
      @RequestBody ObjectRequestBody requestBody) {
    auditor.audit(
        () -> storage.copyObject(project, requestBody.name(), object),
        principal,
        COPY_OBJECT,
        Map.of(PROJECT, project, "from", object, "to", requestBody.name()));
  }

  @Operation(
      summary = "Create a view from an existing table in another project",
      description =
          "The view you're creating will be a symbolic link to selected variables of an existing table. It will"
              + "look and respond like a table, but it will not take up duplicated resources")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Link successfully created"),
        @ApiResponse(responseCode = "404", description = "Unknown project or object"),
        @ApiResponse(responseCode = "409", description = "Object already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(
      value = "/projects/{project}/objects/link",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void createLinkedObject(
      Principal principal,
      @PathVariable String project,
      @RequestBody LinkedObjectRequestBody requestBody) {
    var variableList =
        Optional.ofNullable(requestBody.variables()).map(it -> it.split(",")).stream()
            .flatMap(Arrays::stream)
            .map(String::trim)
            .toList();
    auditor.audit(
        () -> {
          try {
            storage.createLinkedObject(
                requestBody.sourceProject(),
                requestBody.sourceObjectName(),
                requestBody.linkedObject(),
                project,
                requestBody.variables());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        },
        principal,
        CREATE_LINKED_OBJECT,
        Map.of(
            PROJECT,
            project,
            OBJECT,
            requestBody.linkedObject() + LINK_FILE,
            "source",
            concat(concat(project, "/"), requestBody.linkedObject()),
            "columns",
            variableList));
  }

  @Operation(
      summary = "Move an object within a project",
      description =
          "The request body should contain the new object's name in full (e.g. core/nonrep.parquet)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object moved successfully"),
        @ApiResponse(responseCode = "404", description = "Unknown project or object"),
        @ApiResponse(responseCode = "409", description = "Object already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(
      value = "/projects/{project}/objects/{object}/move",
      consumes = {APPLICATION_JSON_VALUE})
  @ResponseStatus(NO_CONTENT)
  public void moveObject(
      Principal principal,
      @PathVariable String project,
      @PathVariable String object,
      @Valid @RequestBody ObjectRequestBody requestBody) {
    auditor.audit(
        () -> storage.moveObject(project, requestBody.name(), object),
        principal,
        MOVE_OBJECT,
        Map.of(PROJECT, project, "from", object, "to", requestBody.name()));
  }

  @Operation(summary = "Object exists?")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object exists"),
        @ApiResponse(responseCode = "404", description = "Object does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @RequestMapping(value = "/projects/{project}/objects/{object}", method = HEAD)
  public ResponseEntity<Void> objectExists(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    boolean objectExists =
        auditor.audit(
            () -> storage.hasObject(project, object),
            principal,
            GET_OBJECT,
            Map.of(PROJECT, project, OBJECT, object));
    return objectExists ? noContent().build() : notFound().build();
  }

  @Operation(summary = "Retrieve first 10 rows of the data")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Preview success"),
        @ApiResponse(responseCode = "404", description = "Object does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(
      path = "/projects/{project}/objects/{object}/preview",
      produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<Map<String, String>> previewObject(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    return auditor.audit(
        () -> storage.getPreview(project, object),
        principal,
        PREVIEW_OBJECT,
        Map.of(PROJECT, project, OBJECT, object));
  }

  @Operation(summary = "Retrieve metadata of table")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Metadata successfully determined"),
        @ApiResponse(responseCode = "404", description = "Table does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(
      path = "/projects/{project}/objects/{object}/metadata",
      produces = APPLICATION_JSON_VALUE)
  public Map<String, ArmadilloColumnMetaData> getMetadataOfTable(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    return auditor.audit(
        () -> {
          try {
            return storage.getMetadata(project, object);
          } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
          }
        },
        principal,
        PREVIEW_METADATA,
        Map.of(PROJECT, project, OBJECT, object));
  }

  @Operation(summary = "Get information of a file")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Retrieval successful"),
        @ApiResponse(responseCode = "404", description = "Object does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(path = "/projects/{project}/objects/{object}/info", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody FileInfo getObjectInfo(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    return auditor.audit(
        () -> storage.getInfo(project, object),
        principal,
        GET_OBJECT_INFO,
        Map.of(PROJECT, project, OBJECT, object));
  }

  @Operation(summary = "Delete an object")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Object deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Unknown project or object"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @DeleteMapping("/projects/{project}/objects/{object}")
  @ResponseStatus(NO_CONTENT)
  public void deleteObject(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    auditor.audit(
        () -> storage.deleteObject(project, object),
        principal,
        DELETE_OBJECT,
        Map.of(PROJECT, project, OBJECT, object));
  }

  @Operation(summary = "Download an object")
  @PreAuthorize("hasRole('ROLE_SU')")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Object downloaded successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Unknown project or object",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping(value = "/projects/{project}/objects/{object}")
  public ResponseEntity<InputStreamResource> downloadObject(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    try {
      return auditDownloadObject(project, object, principal, DOWNLOAD_OBJECT);
    } catch (UnknownObjectException | UnknownProjectException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @Operation(summary = "Download a resource with internal token")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Resource downloaded successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Unknown project or object",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping(value = "/projects/{project}/rawfiles/{object}")
  public ResponseEntity<InputStreamResource> downloadResource(
      Principal principal,
      HttpServletRequest request,
      @PathVariable String project,
      @PathVariable String object) {
    try {
      Map<String, Object> data = new HashMap<>(Map.of(PROJECT, project, OBJECT, object));
      if (principal.getClass() == JwtAuthenticationToken.class) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) principal;
        return downLoadResourceWithToken(token, project, object, data);
      } else {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Token must be issued by armadillo application with correct permissions");
      }
    } catch (UnknownObjectException | UnknownProjectException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (ResponseStatusException e) {
      throw new ResponseStatusException(e.getStatusCode(), e.getMessage());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  ResponseEntity<InputStreamResource> downLoadResourceWithToken(
      JwtAuthenticationToken token, String project, String object, Map<String, Object> data) {
    Map<String, Object> claims = token.getTokenAttributes();
    String errorMsg = "Token must be issued by armadillo application with correct permissions";
    if (claims.get("iss").equals("armadillo-internal")) {
      if (claims.get("resource_project").equals(project)) {
        String resourceObj = object.split("\\.")[0].toLowerCase();
        if (claims.get("resource_object").toString().toLowerCase().equals(resourceObj)) {
          return auditDownloadObject(project, object, token, DOWNLOAD_RESOURCE);
        } else {
          errorMsg = "Token has no permissions for resource object:" + object;
          auditFailure(errorMsg, data, token);
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMsg);
        }
      } else {
        errorMsg = "Token has no permissions for resource project:" + project;
        auditFailure(errorMsg, data, token);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMsg);
      }
    } else {
      auditFailure(errorMsg, data, token);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMsg);
    }
  }

  private ResponseEntity<InputStreamResource> getObject(String project, String object) {
    try {
      var inputStream = storage.loadObject(project, object);
      var objectParts = object.split("/");
      var fileName = objectParts[objectParts.length - 1];
      InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
      ContentDisposition contentDisposition =
          ContentDisposition.attachment().filename(fileName).build();
      var fileSize =
          storage.getFileSizeIfObjectExists(
              ArmadilloStorageService.SHARED_PREFIX + project, object);
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setContentDisposition(contentDisposition);
      httpHeaders.setContentLength(fileSize);
      httpHeaders.setContentType(APPLICATION_OCTET_STREAM);
      return new ResponseEntity<>(inputStreamResource, httpHeaders, HttpStatus.OK);
    } catch (IOException e) {
      throw new FileProcessingException();
    }
  }

  private ResponseEntity<InputStreamResource> auditDownloadObject(
      String project, String object, Principal principal, String type) {
    return auditor.audit(
        () -> getObject(project, object),
        principal,
        type,
        Map.of(PROJECT, project, OBJECT, object));
  }

  private String getSessionFromRequest(HttpServletRequest request) {
    return request.getSession(false) != null ? request.getSession(false).getId() : null;
  }

  private void auditFailure(
      String errorMsg, Map<String, Object> data, JwtAuthenticationToken principal) {
    data.put(MESSAGE, errorMsg);
    data.put(TYPE, ResponseStatusException.class.getSimpleName());
    auditor.audit(principal, DOWNLOAD_RESOURCE + "_FAILURE", data);
  }

  @Operation(summary = "Retrieve columns of parquet file")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Return variables"),
        @ApiResponse(responseCode = "404", description = "Object does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(
      path = "/projects/{project}/objects/{object}/variables",
      produces = APPLICATION_JSON_VALUE)
  public @ResponseBody List<String> getVariables(
      Principal principal, @PathVariable String project, @PathVariable String object) {
    return auditor.audit(
        () -> storage.getVariables(project, object),
        principal,
        GET_VARIABLES,
        Map.of(PROJECT, project, OBJECT, object));
  }
}
