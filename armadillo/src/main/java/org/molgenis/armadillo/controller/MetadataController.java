package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.*;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "metadata",
    description = "API to list and change access metadata of your armadillo instance")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("metadata")
public class MetadataController {

  private final ArmadilloMetadataService armadilloMetadataService;
  private final AuditEventPublisher auditEventPublisher;

  public MetadataController(
      ArmadilloMetadataService armadilloMetadataService, AuditEventPublisher auditEventPublisher) {
    this.armadilloMetadataService = armadilloMetadataService;
    this.auditEventPublisher = auditEventPublisher;
  }

  @Operation(summary = "Get all metadata")
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ArmadilloMetadata settingsRaw(Principal principal) {
    auditEventPublisher.audit(principal, LIST_PROJECTS, Map.of());
    return armadilloMetadataService.settingsList();
  }

  @Operation(summary = "List all permissions")
  @GetMapping(path = "permissions", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Set<ProjectPermission> permissionList(Principal principal) {
    auditEventPublisher.audit(principal, PERMISSIONS_LIST, Map.of());
    return armadilloMetadataService.permissionsList();
  }

  @Operation(
      summary = "Grant access to email on one project",
      description =
          "Permissions will be in effect when user signs in again. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @PostMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(CREATED)
  public void permissionsAdd(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    armadilloMetadataService.permissionsAdd(email, project);
    auditEventPublisher.audit(principal, PERMISSIONS_ADD, Map.of(PROJECT, project, EMAIL, email));
  }

  @Operation(
      summary = "Revoke access to email on one project",
      description =
          "Permissions will be in effect when user signs in again. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @DeleteMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void permissionsDelete(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    armadilloMetadataService.permissionsDelete(email, project);
    auditEventPublisher.audit(
        principal, PERMISSIONS_DELETE, Map.of(PROJECT, project, EMAIL, email));
  }

  @Operation(
      summary = "List projects (key) and per project list user emails array having access (value)")
  @GetMapping(path = "projects", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProjectDetails> projectList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_PROJECTS, Map.of());
    return armadilloMetadataService.projectsList();
  }

  @Operation(summary = "Get project by name")
  @GetMapping(value = "projects/{projectName}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ProjectDetails projectGetByProjectName(
      Principal principal, @PathVariable String projectName) {
    auditEventPublisher.audit(principal, GET_PROJECT, Map.of(PROJECT, projectName));
    return armadilloMetadataService.projectsByName(projectName);
  }

  @Operation(summary = "Delete project including permissions and data")
  @DeleteMapping(value = "projects/{project}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsDelete(Principal principal, @PathVariable String project) {
    armadilloMetadataService.projectsDelete(project);
    auditEventPublisher.audit(principal, DELETE_PROJECT, Map.of(PROJECT, project));
  }

  @Operation(summary = "Add or update project including permissions")
  @PutMapping(value = "projects", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsUpsert(Principal principal, @RequestBody ProjectDetails projectDetails) {
    armadilloMetadataService.projectsUpsert(projectDetails);
    auditEventPublisher.audit(principal, UPSERT_PROJECT, Map.of(PROJECT, projectDetails));
  }

  @Operation(
      summary =
          "List users (key) and per user list details, such as firstName, lastName and projects with permission",
      description = " projects:['administrators',...] means user is SU")
  @GetMapping(path = "users", produces = APPLICATION_JSON_VALUE)
  public List<UserDetails> userList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_USERS, Map.of());
    return armadilloMetadataService.usersList();
  }

  @Operation(summary = "Get user  using email as id")
  @GetMapping(value = "users/{email}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public UserDetails userByEmail(Principal principal, @PathVariable String email) {
    Objects.requireNonNull(email);
    auditEventPublisher.audit(principal, GET_USER, Map.of(EMAIL, email));
    return armadilloMetadataService.usersByEmail(email);
  }

  @Operation(summary = "Add/Update user by email using email as id")
  @PutMapping(value = "users", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userUpsert(Principal principal, @RequestBody UserDetails userDetails) {
    armadilloMetadataService.userUpsert(userDetails);
    auditEventPublisher.audit(principal, UPSERT_USER, Map.of("user", userDetails));
  }

  @Operation(summary = "Delete user including details and permissions using email as id")
  @DeleteMapping(value = "users/{email}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userDelete(Principal principal, @PathVariable String email) {
    armadilloMetadataService.userDelete(email);
    auditEventPublisher.audit(principal, DELETE_USER, Map.of(EMAIL, email));
  }
}
