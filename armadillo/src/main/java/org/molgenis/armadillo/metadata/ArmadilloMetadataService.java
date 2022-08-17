package org.molgenis.armadillo.metadata;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ArmadilloMetadataService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloMetadataService.class);
  public static final String METADATA_FILE = "metadata.json";
  private ArmadilloMetadata settings;
  private final ArmadilloStorageService storage;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${datashield.oidc-permission-enabled}")
  private boolean oidcPermissionsEnabled;

  public ArmadilloMetadataService(ArmadilloStorageService armadilloStorageService) {
    Objects.requireNonNull(armadilloStorageService);
    this.storage = armadilloStorageService;
    this.reload();
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
    return this.settings;
  }

  public List<UserDetails> usersList() {
    return settings.getUsers().keySet().stream().map(this::usersByEmail).toList();
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
                    .putIfAbsent(projectName, ProjectDetails.create(projectName, null));
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
    Objects.requireNonNull(email);
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

  /** key is project, value list of users */
  public List<ProjectDetails> projectsList() {
    return settings.getProjects().keySet().stream().map(this::projectsByName).toList();
  }

  public ProjectDetails projectsByName(String projectName) {
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
    projectDetails = ProjectDetails.create(projectName, null);
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
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);

    settings.getUsers().putIfAbsent(email, UserDetails.create(email, null, null, null, null, null));
    settings.getProjects().putIfAbsent(project, ProjectDetails.create(project, null));
    settings.getPermissions().add(ProjectPermission.create(email, project));

    save();
  }

  public synchronized void permissionsDelete(String email, String project) {

    Objects.requireNonNull(email);
    Objects.requireNonNull(project);

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

  public UserDetails usersByEmail(String email) {
    UserDetails userDetails =
        settings
            .getUsers()
            .getOrDefault(email, UserDetails.create(email, null, null, null, null, null));
    return UserDetails.create(
        userDetails.getEmail(),
        userDetails.getFirstName(),
        userDetails.getLastName(),
        userDetails.getInstitution(),
        userDetails.getAdmin(),
        getPermissionsForEmail(email));
  }

  private synchronized void save() {
    try {
      String json = objectMapper.writeValueAsString(settings);
      try (InputStream inputStream = new ByteArrayInputStream(json.getBytes())) {
        storage.saveSystemFile(inputStream, METADATA_FILE, MediaType.APPLICATION_JSON);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    } finally {
      this.reload();
    }
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

  public void reload() {
    String result;
    try (InputStream inputStream = storage.loadSystemFile(METADATA_FILE)) {
      result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      ArmadilloMetadata temp = objectMapper.readValue(result, ArmadilloMetadata.class);
      settings = Objects.requireNonNullElseGet(temp, ArmadilloMetadata::create);
    } catch (ValueInstantiationException e) {
      // this is serious, manually edited file maybe?
      LOGGER.error(String.format("Parsing of %s failed: %s", METADATA_FILE, e.getMessage()));
      System.exit(-1);
      settings = ArmadilloMetadata.create();
    } catch (Exception e) {
      // this probably just means first time
      settings = ArmadilloMetadata.create();
    }
  }
}
