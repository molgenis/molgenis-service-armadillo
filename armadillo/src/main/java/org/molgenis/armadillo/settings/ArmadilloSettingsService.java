package org.molgenis.armadillo.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
// cannot do global @PreAuthorize(hasRole(SU))
// because anonymous needs to access during login
public class ArmadilloSettingsService {

  // helper for internal use
  public Set<String> getPermissionsForEmail(String email) {
    return settings.getPermissions().stream()
        .filter(projectPermission -> projectPermission.getEmail().equals(email))
        .map(ProjectPermission::getProject)
        .collect(Collectors.toSet());
  }

  public static final String SETTINGS_FILE = "settings.json";
  private ArmadilloSettings settings;
  private final ArmadilloStorageService armadilloStorageService;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public ArmadilloSettingsService(ArmadilloStorageService armadilloStorageService) {
    Objects.requireNonNull(armadilloStorageService);
    this.armadilloStorageService = armadilloStorageService;
    this.reload();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public ArmadilloSettings settingsList() {
    return this.settings;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public List<UserDetails> usersList() {
    return settings.getUsers().keySet().stream()
        .map(this::usersByEmail) // to add the
        .toList();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void userUpsert(UserDetails userDetails) {
    String email = userDetails.getEmail();
    // strip old permissions
    Set<ProjectPermission> permissions =
        settings.getPermissions().stream()
            .filter(permission -> !permission.getEmail().equals(email))
            .collect(Collectors.toSet());
    // add new permissions
    if (userDetails.getProjects() != null) {
      permissions.addAll(
          userDetails.getProjects().stream()
              .map(project -> ProjectPermission.create(email, project))
              .collect(Collectors.toSet()));
    }
    // clear permissions from value object and save
    userDetails =
        UserDetails.create(
            userDetails.getEmail(),
            userDetails.getFirstName(),
            userDetails.getLastName(),
            userDetails.getInstitution(),
            null);
    settings.getUsers().put(userDetails.getEmail(), userDetails);
    settings =
        ArmadilloSettings.create(settings.getUsers(), settingsList().getProjects(), permissions);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void userDelete(String email) {
    Objects.requireNonNull(email);
    // replace settings
    settings.getUsers().remove(email);
    settings =
        ArmadilloSettings.create(
            settings.getUsers(),
            settings.getProjects(),
            // strip from permissions
            settings.getPermissions().stream()
                .filter(permission -> !permission.getEmail().equals(email))
                .collect(Collectors.toSet()));
    save();
  }

  /** key is project, value list of users */
  @PreAuthorize("hasRole('ROLE_SU')")
  public List<ProjectDetails> projectsList() {
    return settings.getProjects().keySet().stream().map(this::projectsByName).toList();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public ProjectDetails projectsByName(String projectName) {
    return ProjectDetails.create(
        projectName,
        // add permissions
        settings.getPermissions().stream()
            .filter(projectPermission -> projectPermission.getProject().equals(projectName))
            .map(ProjectPermission::getEmail)
            .collect(Collectors.toSet()));
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void projectsUpsert(ProjectDetails projectDetails) {
    String projectName = projectDetails.projectName();
    // strip old permissions
    Set<ProjectPermission> permissions =
        settings.getPermissions().stream()
            .filter(permission -> !permission.getProject().equals(projectName))
            .collect(Collectors.toSet());
    // add new permissions
    if (projectDetails.getUsers() != null) {
      permissions.addAll(
          projectDetails.getUsers().stream()
              .map(email -> ProjectPermission.create(email, projectName))
              .collect(Collectors.toSet()));
    }
    // clear permissions from value object and save
    projectDetails = ProjectDetails.create(projectName, null);
    settings.getProjects().put(projectName, projectDetails);
    settings =
        ArmadilloSettings.create(settings.getUsers(), settingsList().getProjects(), permissions);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void projectsDelete(String projectName) {
    settings.getProjects().remove(projectName);
    settings =
        ArmadilloSettings.create(
            settings.getUsers(),
            settings.getProjects(),
            // strip from permissions
            settings.getPermissions().stream()
                .filter(permission -> !permission.getProject().equals(projectName))
                .collect(Collectors.toSet()));
    this.save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public Set<ProjectPermission> permissionsList() {
    return settings.getPermissions();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public synchronized void permissionsAdd(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);

    settings.getUsers().putIfAbsent(email, UserDetails.create(email, null, null, null, null));
    settings.getProjects().putIfAbsent(project, ProjectDetails.create(project, null));
    settings.getPermissions().add(ProjectPermission.create(email, project));

    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public synchronized void permissionsDelete(String email, String project) {

    Objects.requireNonNull(email);
    Objects.requireNonNull(project);

    settings =
        ArmadilloSettings.create(
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
        settings.getUsers().getOrDefault(email, UserDetails.create(email, null, null, null, null));
    return UserDetails.create(
        userDetails.getEmail(),
        userDetails.getFirstName(),
        userDetails.getLastName(),
        userDetails.getInstitution(),
        // as convenience, we add permissions to each user
        getPermissionsForEmail(email));
  }

  private synchronized void save() {
    try {
      String json = objectMapper.writeValueAsString(settings);
      InputStream inputStream = new ByteArrayInputStream(json.getBytes());
      armadilloStorageService.saveSystemFile(
          inputStream, SETTINGS_FILE, MediaType.APPLICATION_JSON);
    } catch (Exception e) {
      throw new StorageException(e);
    } finally {
      this.reload();
    }
  }

  public void reload() {
    String result;
    try (InputStream inputStream = armadilloStorageService.loadSystemFile(SETTINGS_FILE)) {
      result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      ArmadilloSettings temp = objectMapper.readValue(result, ArmadilloSettings.class);
      settings = Objects.requireNonNullElseGet(temp, ArmadilloSettings::create);
    } catch (ValueInstantiationException e) {
      // this is serious, manually edited file maybe?
      e.printStackTrace();
      System.exit(-1);
      settings = ArmadilloSettings.create();
    } catch (Exception e) {
      // this probably just means first time
      settings = ArmadilloSettings.create();
    }
  }
}
