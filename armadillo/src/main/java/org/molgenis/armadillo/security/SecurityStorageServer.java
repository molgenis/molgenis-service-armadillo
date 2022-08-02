package org.molgenis.armadillo.security;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SecurityStorageServer {

  public static final String SETTINGS_FILE = "users.json";
  private Map<String, User> currentUsers = new LinkedHashMap<>();
  @Autowired private ArmadilloStorageService armadilloStorageService;
  private boolean forceReload = true;

  @PreAuthorize("hasRole('ROLE_SU')")
  public Map<String, User> userList() {
    return currentUsers;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  /** key is project, value list of users */
  public Map<String, Set<String>> projectList() {
    reloadIfNeeded();
    Map<String, Set<String>> result = new LinkedHashMap<>();
    currentUsers.forEach(
        (String user, User userDetails) -> {
          userDetails
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

  @PreAuthorize("hasRole('ROLE_SU')")
  public synchronized void accessAdd(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    User user = currentUsers.getOrDefault(email, new User());
    user.getProjects().add(project);
    currentUsers.put(email, user);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public synchronized void accessDelete(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    User user = currentUsers.getOrDefault(email, new User());
    user.getProjects().remove(project);
    currentUsers.put(email, user);
    save();
  }

  public Set<String> getGrantsForEmail(String email) {
    reloadIfNeeded();
    return currentUsers.get(email).getProjects();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void userUpsert(String email, User user) {
    currentUsers.put(email, user);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void userDelete(String email) {
    Objects.requireNonNull(email);
    currentUsers.remove(email);
    save();
  }

  public synchronized void save() {
    String json = new Gson().toJson(currentUsers);
    InputStream inputStream = new ByteArrayInputStream(json.getBytes());
    armadilloStorageService.saveSystemFile(inputStream, SETTINGS_FILE, MediaType.APPLICATION_JSON);
    forceReload = true;
  }

  public void reloadIfNeeded() {
    if (forceReload) {
      InputStream inputStream = armadilloStorageService.loadSystemFile(SETTINGS_FILE);

      Map<String, User> temp =
          new Gson()
              .fromJson(
                  new InputStreamReader(inputStream),
                  new TypeToken<Map<String, User>>() {}.getType());
      if (temp == null) {
        currentUsers = new LinkedHashMap<>();
      } else {
        currentUsers = new LinkedHashMap<>();
        temp.forEach((project, userDetails) -> currentUsers.put(project, userDetails));
      }

      forceReload = false;
    }
  }
}
