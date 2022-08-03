package org.molgenis.armadillo.settings;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class ArmadilloSettingsService {

  public static final String SETTINGS_FILE = "settings.json";
  private ArmadilloSettings settings;
  private final ArmadilloStorageService armadilloStorageService;
  private boolean forceReload = true;

  public ArmadilloSettingsService(ArmadilloStorageService armadilloStorageService) {
    Objects.requireNonNull(armadilloStorageService);
    this.armadilloStorageService = armadilloStorageService;
  }

  public Map<String, UserDetails> userList() {
    return settings.getUsers();
  }

  /** key is project, value list of users */
  public Map<String, Set<String>> projectList() {
    reloadIfNeeded();
    Map<String, Set<String>> result = new LinkedHashMap<>();
    settings
        .getUsers()
        .forEach(
            (String user, UserDetails userDetailsDetails) -> {
              userDetailsDetails
                  .getProjects()
                  .forEach(
                      (String project) -> {
                        Set<String> users = result.getOrDefault(project, new HashSet<>());
                        users.add(user);
                        result.put(project, users);
                      });
            });

    return Collections.unmodifiableMap(result);
  }

  public synchronized void accessAdd(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    UserDetails userDetails = settings.getUsers().getOrDefault(email, UserDetails.create());
    userDetails.getProjects().add(project);
    settings.getUsers().put(email, userDetails);
    save();
  }

  public synchronized void accessDelete(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    UserDetails userDetails = settings.getUsers().getOrDefault(email, UserDetails.create());
    userDetails.getProjects().remove(project);
    settings.getUsers().put(email, userDetails);
    save();
  }

  public Set<String> getGrantsForEmail(String email) {
    reloadIfNeeded();
    return settings.getUsers().getOrDefault(email, UserDetails.create()).getProjects();
  }

  public void userUpsert(String email, UserDetails userDetails) {
    settings.getUsers().put(email, userDetails);
    save();
  }

  public void userDelete(String email) {
    Objects.requireNonNull(email);
    reloadIfNeeded();
    settings.getUsers().remove(email);
    save();
  }

  private synchronized void save() {
    String json = new Gson().toJson(settings);
    InputStream inputStream = new ByteArrayInputStream(json.getBytes());
    armadilloStorageService.saveSystemFile(inputStream, SETTINGS_FILE, MediaType.APPLICATION_JSON);
    forceReload = true;
  }

  private void reloadIfNeeded() {
    if (forceReload) {
      InputStream inputStream = armadilloStorageService.loadSystemFile(SETTINGS_FILE);

      ArmadilloSettings temp =
          new Gson().fromJson(new InputStreamReader(inputStream), ArmadilloSettings.class);

      if (temp == null) {
        settings = ArmadilloSettings.create();
      } else {
        settings = temp;
      }

      forceReload = false;
    }
  }
}
