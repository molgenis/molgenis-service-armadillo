package org.molgenis.armadillo.metadata;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.molgenis.armadillo.exceptions.UnknownUserException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ArmadilloMetadataService {

  private ArmadilloMetadata settings;
  private final ArmadilloStorageService storage;
  private final MetadataLoader loader;

  @Value("${datashield.oidc-permission-enabled}")
  private boolean oidcPermissionsEnabled;

  private final String adminUser;
  private final String defaultProject;

  public ArmadilloMetadataService(
      ArmadilloStorageService armadilloStorageService,
      MetadataLoader metadataLoader,
      @Value("${datashield.bootstrap.oidc-admin-user}") String adminUser,
      @Value("${datashield.bootstrap.default-project}") String defaultProject) {
    this.loader = requireNonNull(metadataLoader);
    this.storage = requireNonNull(armadilloStorageService);
    this.adminUser = adminUser;
    this.defaultProject = defaultProject;
    runAsSystem(this::initialize);
  }

  /**
   * Initialization separated from constructor so that it can be called in WemMvc tests
   * <strong>after</strong> mocks have been initialized.
   */
  public void initialize() {
    settings = loader.load();
    bootstrap();
  }

  public Collection<GrantedAuthority> getAuthoritiesForEmail(
      String email, Map<String, Object> claims) {
    List<GrantedAuthority> result = new ArrayList<>();

    // optionally, we will extract roles from claims
    // in this case you can define roles centrally
    if (oidcPermissionsEnabled) {
      result.addAll(
          ((Collection<?>) claims.getOrDefault("roles", emptyList()))
              .stream()
                  .map(Object::toString)
                  .map(role -> "ROLE_" + role.toUpperCase())
                  .map(SimpleGrantedAuthority::new)
                  .toList());
    }

    // claims from local permissions store
    result.addAll(
        this.getPermissionsForEmail(email).stream()
            .map(project -> "ROLE_" + project.toUpperCase() + "_RESEARCHER")
            .map(SimpleGrantedAuthority::new)
            .toList());

    // claims from user 'admin' property
    if (this.isSuperUser(email)) {
      result.add(new SimpleGrantedAuthority("ROLE_SU"));
    }

    return result;
  }

  public ArmadilloMetadata settingsList() {
    return ArmadilloMetadata.create(
        new ConcurrentHashMap<>(usersMap()),
        new ConcurrentHashMap<>(projectsMap()),
        settings.getPermissions());
  }

  /**
   * Gets a map of email -> UserDetails. Permissions are not stored in the user, so they are
   * injected here.
   */
  private Map<String, UserDetails> usersMap() {
    return settings.getUsers().keySet().stream()
        .map(this::usersByEmail)
        .collect(Collectors.toMap(UserDetails::getEmail, u -> u));
  }

  public List<UserDetails> usersList() {
    return new ArrayList<>(usersMap().values());
  }

  public void userUpsert(UserDetails userDetails) {
    String email = userDetails.getEmail();
    // strip previous permissions
    Set<ProjectPermission> permissions =
        settings.getPermissions().stream()
            .filter(permission -> !permission.getEmail().equals(email))
            .collect(Collectors.toSet());
    // add replace with permissions
    if (userDetails.getProjects() != null) {
      // add missing projects
      userDetails
          .getProjects()
          .forEach(
              projectName -> {
                storage.upsertProject(projectName);

                // add missing project, if applicable
                settings
                    .getProjects()
                    .putIfAbsent(projectName, ProjectDetails.create(projectName, new HashSet<>()));
                // add permission to that project
                permissions.add(ProjectPermission.create(email, projectName));
              });
    }

    // clear permissions from value object and save
    userDetails =
        UserDetails.create(
            userDetails.getEmail(),
            userDetails.getFirstName(),
            userDetails.getLastName(),
            userDetails.getInstitution(),
            userDetails.getAdmin(),
            null // stored in permissions
            );
    // update users
    settings.getUsers().put(userDetails.getEmail(), userDetails);
    // replace permissions
    settings = ArmadilloMetadata.create(settings.getUsers(), settings.getProjects(), permissions);
    save();
  }

  public void userDelete(String email) {
    requireNonNull(email);

    if (!settings.getUsers().containsKey(email)) {
      throw new UnknownUserException(email);
    }

    // replace settings
    settings.getUsers().remove(email);
    settings =
        ArmadilloMetadata.create(
            settings.getUsers(),
            settings.getProjects(),
            // strip from permissions
            settings.getPermissions().stream()
                .filter(permission -> !permission.getEmail().equals(email))
                .collect(Collectors.toSet()));
    save();
  }

  /**
   * Gets a map of name -> ProjectDetails. Permissions are not stored in the project, so they are
   * injected here.
   */
  private Map<String, ProjectDetails> projectsMap() {
    return settings.getProjects().keySet().stream()
        .map(this::projectsByName)
        .collect(Collectors.toMap(ProjectDetails::getName, p -> p));
  }

  public List<ProjectDetails> projectsList() {
    return new ArrayList<>(projectsMap().values());
  }

  public ProjectDetails projectsByName(String projectName) {
    if (!settings.getProjects().containsKey(projectName)) {
      throw new UnknownProjectException(projectName);
    }

    return ProjectDetails.create(
        projectName,
        // add permissions
        settings.getPermissions().stream()
            .filter(projectPermission -> projectPermission.getProject().equals(projectName))
            .map(ProjectPermission::getEmail)
            .collect(Collectors.toSet()));
  }

  public void projectsUpsert(ProjectDetails projectDetails) {
    String projectName = projectDetails.getName();

    storage.upsertProject(projectName);

    // strip previous permissions for this project
    Set<ProjectPermission> permissions =
        settings.getPermissions().stream()
            .filter(permission -> !permission.getProject().equals(projectName))
            .collect(Collectors.toSet());

    // add current permissions for this project
    if (projectDetails.getUsers() != null) {
      projectDetails
          .getUsers()
          .forEach(
              userEmail -> {
                // add missing users, if applicable
                settings.getUsers().putIfAbsent(userEmail, UserDetails.create(userEmail));
                // add permission
                permissions.add(ProjectPermission.create(userEmail, projectName));
              });
    }

    // clone projectDetails to strip permissions from value object and save
    // (permissions are saved separately)
    projectDetails = ProjectDetails.create(projectName, Collections.emptySet());
    settings.getProjects().put(projectName, projectDetails);
    settings = ArmadilloMetadata.create(settings.getUsers(), settings.getProjects(), permissions);
    save();
  }

  public void projectsDelete(String projectName) {
    storage.deleteProject(projectName);

    settings.getProjects().remove(projectName);
    settings =
        ArmadilloMetadata.create(
            settings.getUsers(),
            settings.getProjects(),
            // strip from permissions
            settings.getPermissions().stream()
                .filter(permission -> !permission.getProject().equals(projectName))
                .collect(Collectors.toSet()));
    this.save();
  }

  public Set<ProjectPermission> permissionsList() {
    return settings.getPermissions();
  }

  public synchronized void permissionsAdd(String email, String project) {
    requireNonNull(email);
    requireNonNull(project);

    settings.getUsers().putIfAbsent(email, UserDetails.create(email, null, null, null, null, null));
    settings.getProjects().putIfAbsent(project, ProjectDetails.create(project, null));
    settings.getPermissions().add(ProjectPermission.create(email, project));

    save();
  }

  public synchronized void permissionsDelete(String email, String project) {

    requireNonNull(email);
    requireNonNull(project);

    settings =
        ArmadilloMetadata.create(
            settings.getUsers(),
            settings.getProjects(),
            settings.getPermissions().stream()
                .filter(
                    projectPermission ->
                        !projectPermission.getProject().equals(project)
                            && !projectPermission.getEmail().equals(email))
                .collect(Collectors.toSet()));

    save();
  }

  public UserDetails userByEmail(String email) {
    if (!settings.getUsers().containsKey(email)) {
      throw new UnknownUserException(email);
    }

    UserDetails userDetails = settings.getUsers().get(email);
    return UserDetails.create(
        userDetails.getEmail(),
        userDetails.getFirstName(),
        userDetails.getLastName(),
        userDetails.getInstitution(),
        userDetails.getAdmin(),
        getPermissionsForEmail(email));
  }

  private void save() {
    settings = loader.save(settings);
  }

  private Set<String> getPermissionsForEmail(String email) {
    return settings.getPermissions().stream()
        .filter(projectPermission -> projectPermission.getEmail().equals(email))
        .map(ProjectPermission::getProject)
        .collect(Collectors.toSet());
  }

  private boolean isSuperUser(String email) {
    return settings.getUsers().containsKey(email)
        && Boolean.TRUE.equals(settings.getUsers().get(email).getAdmin());
  }

  private void bootstrap() {
    if (adminUser != null && !settings.getUsers().containsKey(adminUser)) {
      userUpsert(UserDetails.createAdmin(adminUser));
    }

    if (defaultProject != null && !settings.getProjects().containsKey(defaultProject)) {
      projectsUpsert(ProjectDetails.create(defaultProject, emptySet()));
    }
  }
}
