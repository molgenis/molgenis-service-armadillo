package org.molgenis.armadillo.controller;

import static java.lang.String.format;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.*;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "access", description = "Access API to manage users, projects, and permissions")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("access")
public class AccessController {

  private final AccessService metadata;
  private final AuditEventPublisher auditor;
  private final ArmadilloStorageService storage;

  public AccessController(
      AccessService metadataService, AuditEventPublisher auditor, ArmadilloStorageService storage) {
    this.metadata = metadataService;
    this.auditor = auditor;
    this.storage = storage;
  }

  @Operation(summary = "Get all metadata")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All metadata listed",
            content = @Content(schema = @Schema(implementation = AccessMetadata.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public AccessMetadata settingsRaw(Principal principal) {
    return auditor.audit(metadata::settingsList, principal, LIST_ACCESS_DATA, Map.of());
  }

  @Operation(summary = "List all permissions")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All permissions listed",
            content =
                @Content(
                    array =
                        @ArraySchema(schema = @Schema(implementation = ProjectPermission.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "permissions", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public Set<ProjectPermission> permissionList(Principal principal) {
    return auditor.audit(metadata::permissionsList, principal, PERMISSIONS_LIST, Map.of());
  }

  @Operation(
      summary = "Grant access to email on one project",
      description = "Permissions will be in effect when user signs in again.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Access granted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void permissionsAdd(
      Principal principal, @Valid @RequestBody ProjectPermission permission) {
    auditor.audit(
        () -> metadata.permissionsAdd(permission.getEmail(), permission.getProject()),
        principal,
        PERMISSIONS_ADD,
        Map.of(PROJECT, permission.getProject(), EMAIL, permission.getEmail()));
  }

  @Operation(
      summary = "Revoke access to email on one project",
      description = "Permissions will be in effect when user signs in again.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Access revoked"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @DeleteMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void permissionsDelete(
      Principal principal, @Valid @RequestBody ProjectPermission permission) {
    auditor.audit(
        () -> metadata.permissionsDelete(permission.getEmail(), permission.getProject()),
        principal,
        PERMISSIONS_DELETE,
        Map.of(PROJECT, permission.getProject(), EMAIL, permission.getEmail()));
  }

  @Operation(
      summary = "List projects (key) and per project list user emails array having access (value)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All projects listed",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ProjectDetails.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "projects", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ProjectDetails> projectList(Principal principal) {
    return auditor.audit(metadata::projectsList, principal, LIST_PROJECTS, Map.of());
  }

  @Operation(summary = "Get project by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Project listed",
            content = @Content(schema = @Schema(implementation = ProjectDetails.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Project does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(value = "projects/{projectName}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public ProjectDetails projectGetByProjectName(
      Principal principal, @PathVariable String projectName) {
    return auditor.audit(
        () -> metadata.projectsByName(projectName),
        principal,
        GET_PROJECT,
        Map.of(PROJECT, projectName));
  }

  @Operation(summary = "Delete project including permissions and data")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Project deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "Project does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "projects/{project}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void projectsDelete(Principal principal, @PathVariable String project) {
    auditor.audit(
        () -> metadata.projectsDelete(project),
        principal,
        DELETE_PROJECT,
        Map.of(PROJECT, project));
  }

  @Operation(summary = "Add or update project including permissions")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Project added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(value = "projects", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void projectsUpsert(
      Principal principal, @Valid @RequestBody ProjectDetails projectDetails) {
    auditor.audit(
        () -> metadata.projectsUpsert(projectDetails),
        principal,
        UPSERT_PROJECT,
        Map.of(PROJECT, projectDetails));
  }

  @Operation(
      summary =
          "List users (key) and per user list details, such as firstName, lastName and projects with permission",
      description = " projects:['admin',...] means user is SU")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All users listed",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = UserDetails.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "users", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<UserDetails> userList(Principal principal) {
    return auditor.audit(metadata::usersList, principal, LIST_USERS, Map.of());
  }

  @Operation(summary = "Get user using email as id")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User listed",
            content = @Content(schema = @Schema(implementation = ProjectDetails.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(value = "users/{email}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public UserDetails userByEmail(Principal principal, @PathVariable String email) {
    Objects.requireNonNull(email);
    return auditor.audit(
        () -> metadata.userByEmail(email), principal, GET_USER, Map.of(EMAIL, email));
  }

  @Operation(summary = "Add/Update user by email using email as id")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(value = "users", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void userUpsert(Principal principal, @Valid @RequestBody UserDetails userDetails) {
    auditor.audit(
        () -> metadata.userUpsert(userDetails),
        principal,
        UPSERT_USER,
        Map.of("user", userDetails));
  }

  @Operation(summary = "Delete user including details and permissions using email as id")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "User does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "users/{email}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void userDelete(Principal principal, @PathVariable String email) {
    auditor.audit(() -> metadata.userDelete(email), principal, DELETE_USER, Map.of(EMAIL, email));
  }

  @Operation(
      summary = "Request approval endpoint for access to table variables",
      description =
          "Will create view of selected variables of selected table under projectname of provided requestID and grant access to project for specified user")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Access granted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @PostMapping(path = "request/approve", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void requestAccess(
      Principal principal, @Valid @RequestBody RequestAccessBody requestAccessBody) {
    String requestId = requestAccessBody.getRequestId();
    String tablePath = requestAccessBody.getTable();
    String variables = requestAccessBody.getVariables().toString();
    String user = requestAccessBody.getUser();
    auditor.audit(
        () -> {
          System.out.printf(
              "Request [%s]: Approving access for user [%s] to table [%s], requested variables: [%s]%n",
              requestId, user, tablePath, variables);

          if (storage.hasProject(requestId)) {
            // This is deliberate. We don't want to approve a project that already exists, because
            // that is risking exposure of unrequested data to the requestee.
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                format(
                    "Project for request id [ %s ] already exists. Please remove this project before approving this request.",
                    requestId));
          }
          storage.upsertProject(requestId);
          String[] projectFolderTable = getProjectFolderTableFromPath(tablePath, requestId);
          try {
            storage.createLinkedObject(
                projectFolderTable[0],
                projectFolderTable[1] + "/" + projectFolderTable[2],
                projectFolderTable[1] + "/" + projectFolderTable[2],
                requestId,
                variables);
            metadata.permissionsAdd(user, requestId);
          } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                format(
                    "Cannot create and approve request [%s] for table [%s] and user [%s] because: %s",
                    requestId, tablePath, user, e));
          }
        },
        principal,
        "REQUEST_ACCESS",
        Map.of(
            "REQUEST_ID",
            requestAccessBody.getRequestId(),
            USER,
            requestAccessBody.getUser(),
            TABLE,
            requestAccessBody.getTable(),
            "variables",
            requestAccessBody.getVariables()));
  }

  private static String @NonNull [] getProjectFolderTableFromPath(
      String tablePath, String requestId) {
    String[] projectFolderTable = tablePath.split("/");
    if (projectFolderTable.length != 3) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          format(
              "Cannot fulfill request [%s] because [%s] should consist of project, "
                  + "folder, table in format:\n"
                  + "projectId/folderId/tableId \n"
                  + "Length of current table path is [%s], should be 3.",
              requestId, tablePath, projectFolderTable.length));
    } else {
      // remove parquet extension, if provided
      projectFolderTable[2] = projectFolderTable[2].replace(".parquet", "");
    }
    return projectFolderTable;
  }
}
