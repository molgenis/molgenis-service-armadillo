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
import java.util.Map;
import java.util.Objects;
import javax.validation.Valid;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.settings.ArmadilloSettings;
import org.molgenis.armadillo.settings.ArmadilloSettingsService;
import org.molgenis.armadillo.settings.ProjectDetails;
import org.molgenis.armadillo.settings.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "settings",
    description = "API to list and change access settings of your armadillo instance")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("settings")
public class SettingsController {

  private final ArmadilloSettingsService armadilloSettingsService;
  private final AuditEventPublisher auditEventPublisher;

  public SettingsController(
      ArmadilloSettingsService armadilloSettingsService, AuditEventPublisher auditEventPublisher) {
    this.armadilloSettingsService = armadilloSettingsService;
    this.auditEventPublisher = auditEventPublisher;
  }

  @Operation(summary = "Get all settings")
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ArmadilloSettings settingsList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_PROJECTS, Map.of());
    return armadilloSettingsService.settingsList();
  }

  @Operation(
      summary = "Grant access to email on one project",
      description =
          "Permissions will be in effect when user signs in again. N.B. 'administrators' is a special project which will grant administrator permission to a user")
  @PostMapping(path = "permissions", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(CREATED)
  public void permissionsAdd(
      Principal principal, @RequestParam String email, @RequestParam String project) {
    armadilloSettingsService.permissionsAdd(email, project);
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
    armadilloSettingsService.permissionsDelete(email, project);
    auditEventPublisher.audit(
        principal, PERMISSIONS_DELETE, Map.of(PROJECT, project, EMAIL, email));
  }

  @Operation(
      summary = "List projects (key) and per project list user emails array having access (value)")
  @GetMapping(path = "projects", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Map<String, ProjectDetails> projectList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_PROJECTS, Map.of());
    return armadilloSettingsService.projectsList();
  }

  @Operation(summary = "Get project by name")
  @GetMapping(value = "projects/{projectName}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public ProjectDetails projectGetByProjectName(
      Principal principal, @PathVariable String projectName) {
    auditEventPublisher.audit(principal, GET_PROJECT, Map.of(PROJECT, projectName));
    return armadilloSettingsService.projectsByName(projectName);
  }

  @Operation(summary = "Delete project including permissions and data")
  @DeleteMapping(value = "projects/{project}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsDelete(Principal principal, @PathVariable String project) {
    armadilloSettingsService.projectsDelete(project);
    auditEventPublisher.audit(principal, DELETE_PROJECT, Map.of(PROJECT, project));
  }

  @Operation(summary = "Add or update project including permissions")
  @PutMapping(value = "projects/{project}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void projectsUpsert(
      Principal principal,
      @PathVariable String project,
      @RequestBody ProjectDetails projectDetails) {
    armadilloSettingsService.projectsUpsert(project, projectDetails);
    auditEventPublisher.audit(principal, UPSERT_PROJECT, Map.of(PROJECT, project));
  }

  @Operation(
      summary =
          "List users (key) and per user list details, such as firstName, lastName and projects with permission",
      description = " projects:['administrators',...] means user is SU")
  @GetMapping(path = "users", produces = APPLICATION_JSON_VALUE)
  public Map<String, UserDetails> userList(Principal principal) {
    auditEventPublisher.audit(principal, LIST_USERS, Map.of());
    return armadilloSettingsService.usersList();
  }

  @Operation(summary = "Get user  using email as id")
  @GetMapping(value = "users/{email}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public UserDetails userByEmail(Principal principal, @PathVariable String email) {
    Objects.requireNonNull(email);
    auditEventPublisher.audit(principal, GET_USER, Map.of(EMAIL, email));
    return armadilloSettingsService.usersByEmail(email);
  }

  @Operation(summary = "Add/Update user by email using email as id")
  @PutMapping(value = "users/{email}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userUpsert(
      Principal principal, @PathVariable String email, @RequestBody UserDetails userDetails) {
    armadilloSettingsService.userUpsert(email, userDetails);
    auditEventPublisher.audit(principal, UPSERT_USER, Map.of(EMAIL, email, "user", userDetails));
  }

  @Operation(summary = "Delete user including details and permissions using email as id")
  @DeleteMapping(value = "users/{email}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void userDelete(Principal principal, @PathVariable String email) {
    armadilloSettingsService.userDelete(email);
    auditEventPublisher.audit(principal, DELETE_USER, Map.of(EMAIL, email));
  }
}
