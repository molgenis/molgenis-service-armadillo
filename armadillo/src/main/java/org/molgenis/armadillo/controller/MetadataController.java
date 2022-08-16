package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_USER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.EMAIL;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_USER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_PROJECTS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LIST_USERS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PERMISSIONS_ADD;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PERMISSIONS_DELETE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PERMISSIONS_LIST;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.UPSERT_PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.UPSERT_USER;
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
import org.molgenis.armadillo.metadata.ArmadilloMetadata;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.molgenis.armadillo.metadata.ProjectDetails;
import org.molgenis.armadillo.metadata.ProjectPermission;
import org.molgenis.armadillo.metadata.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin", description = "Admin API to manage users, project and permissions")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("admin")
public class MetadataController {

  private final ArmadilloMetadataService metadata;
  private final AuditEventPublisher auditor;

  public MetadataController(ArmadilloMetadataService metadataService, AuditEventPublisher auditor) {
    this.metadata = metadataService;
    this.auditor = auditor;
  }

  @Operation(summary = "Get all metadata")
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ArmadilloMetadata settingsRaw(Principal principal) {
    return auditor.audit(metadata::settingsList, principal, LIST_PROJECTS, Map.of());
  }

  @Operation(summary = "List all permissions")
  @GetMapping(path = "permissions", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Set<ProjectPermission> permissionList(Principal principal) {
    return auditor.audit(metadata::permissionsList, principal, PERMISSIONS_LIST, Map.of());
  }

  @Operation(
      summary = "Grant access to email on one project",
      description =
          "Permissions will be in effect when user signs in again. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @PostMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(CREATED)
  public void permissionsAdd(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    auditor.audit(
        () -> metadata.permissionsAdd(email, project),
        principal,
        PERMISSIONS_ADD,
        Map.of(PROJECT, project, EMAIL, email));
  }

  @Operation(
      summary = "Revoke access to email on one project",
      description =
          "Permissions will be in effect when user signs in again. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @DeleteMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void permissionsDelete(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    auditor.audit(
        () -> metadata.permissionsDelete(email, project),
        principal,
        PERMISSIONS_DELETE,
        Map.of(PROJECT, project, EMAIL, email));
  }

  @Operation(
      summary = "List projects (key) and per project list user emails array having access (value)")
  @GetMapping(path = "projects", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<ProjectDetails> projectList(Principal principal) {
    return auditor.audit(metadata::projectsList, principal, LIST_PROJECTS, Map.of());
  }

  @Operation(summary = "Get project by name")
  @GetMapping(value = "projects/{projectName}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ProjectDetails projectGetByProjectName(
      Principal principal, @PathVariable String projectName) {
    return auditor.audit(
        () -> metadata.projectsByName(projectName),
        principal,
        GET_PROJECT,
        Map.of(PROJECT, projectName));
  }

  @Operation(summary = "Delete project including permissions and data")
  @DeleteMapping(value = "projects/{project}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsDelete(Principal principal, @PathVariable String project) {
    auditor.audit(
        () -> metadata.projectsDelete(project),
        principal,
        DELETE_PROJECT,
        Map.of(PROJECT, project));
  }

  @Operation(summary = "Add or update project including permissions")
  @PutMapping(value = "projects", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsUpsert(Principal principal, @RequestBody ProjectDetails projectDetails) {
    auditor.audit(
        () -> metadata.projectsUpsert(projectDetails),
        principal,
        UPSERT_PROJECT,
        Map.of(PROJECT, projectDetails));
  }

  @Operation(
      summary =
          "List users (key) and per user list details, such as firstName, lastName and projects with permission",
      description = " projects:['administrators',...] means user is SU")
  @GetMapping(path = "users", produces = APPLICATION_JSON_VALUE)
  public List<UserDetails> userList(Principal principal) {
    return auditor.audit(metadata::usersList, principal, LIST_USERS, Map.of());
  }

  @Operation(summary = "Get user  using email as id")
  @GetMapping(value = "users/{email}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public UserDetails userByEmail(Principal principal, @PathVariable String email) {
    Objects.requireNonNull(email);
    return auditor.audit(
        () -> metadata.usersByEmail(email), principal, GET_USER, Map.of(EMAIL, email));
  }

  @Operation(summary = "Add/Update user by email using email as id")
  @PutMapping(value = "users", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userUpsert(Principal principal, @RequestBody UserDetails userDetails) {
    auditor.audit(
        () -> metadata.userUpsert(userDetails),
        principal,
        UPSERT_USER,
        Map.of("user", userDetails));
  }

  @Operation(summary = "Delete user including details and permissions using email as id")
  @DeleteMapping(value = "users/{email}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userDelete(Principal principal, @PathVariable String email) {
    auditor.audit(() -> metadata.userDelete(email), principal, DELETE_USER, Map.of(EMAIL, email));
  }
}
