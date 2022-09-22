package org.molgenis.armadillo.controller;

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
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin", description = "Admin API to manage users, projects, profiles, and permissions")
@RestController
@Valid
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("admin")
public class AdminController {

  private final ArmadilloMetadataService metadata;
  private final AuditEventPublisher auditor;

  public AdminController(ArmadilloMetadataService metadataService, AuditEventPublisher auditor) {
    this.metadata = metadataService;
    this.auditor = auditor;
  }

  @Operation(summary = "Get all metadata")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All metadata listed",
            content = @Content(schema = @Schema(implementation = ArmadilloMetadata.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public ArmadilloMetadata settingsRaw(Principal principal) {
    return auditor.audit(metadata::settingsList, principal, LIST_PROJECTS, Map.of());
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
      Principal principal, @RequestParam String email, @RequestParam String project) {
    auditor.audit(
        () -> metadata.permissionsAdd(email, project),
        principal,
        PERMISSIONS_ADD,
        Map.of(PROJECT, project, EMAIL, email));
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
      Principal principal, @RequestParam String email, @RequestParam String project) {
    auditor.audit(
        () -> metadata.permissionsDelete(email, project),
        principal,
        PERMISSIONS_DELETE,
        Map.of(PROJECT, project, EMAIL, email));
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
        () -> metadata.usersByEmail(email), principal, GET_USER, Map.of(EMAIL, email));
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
  public void userUpsert(Principal principal, @RequestBody UserDetails userDetails) {
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

  @Operation(summary = "List profiles")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All profiles listed",
            content =
                @Content(
                    array = @ArraySchema(schema = @Schema(implementation = ProfileConfig.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "profiles", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<ProfileConfig> profileList(Principal principal) {
    return auditor.audit(
        () -> {
          return metadata.profileList().stream()
              .map(profile -> metadata.profileByName(profile.getName()))
              .toList();
        },
        principal,
        LIST_PROFILES,
        Map.of());
  }

  @Operation(summary = "Get profile by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile listed",
            content = @Content(schema = @Schema(implementation = ProfileConfig.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Profile does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(value = "profiles/{name}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public ProfileConfig profileGetByProfileName(Principal principal, @PathVariable String name) {
    return auditor.audit(
        () -> metadata.profileByName(name), principal, GET_PROFILE, Map.of(PROFILE, name));
  }

  @Operation(summary = "Add or update profile (if enabled, including docker image)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile added or updated"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @PutMapping(value = "profiles", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(OK)
  public void profileUpsert(Principal principal, @RequestBody ProfileConfig profileConfig)
      throws InterruptedException {
    metadata.profileUpsert(profileConfig);
    auditor.audit(
        () -> {}, // because of the exception that might happen, and cannot be caught
        principal,
        UPSERT_PROFILE,
        Map.of(PROFILE, profileConfig));
  }

  @Operation(summary = "Delete profile (if enabled, including docker image)")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Profile deleted"),
        @ApiResponse(
            responseCode = "404",
            description = "Profile does not exist",
            content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @DeleteMapping(value = "profiles/{name}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(NO_CONTENT)
  public void profileDelete(Principal principal, @PathVariable String name) {
    auditor.audit(
        () -> metadata.profileDelete(name), principal, DELETE_PROFILE, Map.of(PROFILE, name));
  }
}
