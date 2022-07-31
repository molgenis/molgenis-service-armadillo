package org.molgenis.armadillo.security;

import com.google.gson.Gson;
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
public class AccessStorageService {

  public static final String PERMISSIONS_FILE = "permissions.json";
  private Map<String, Set<String>> currentPermissions = new LinkedHashMap<>();
  @Autowired private ArmadilloStorageService armadilloStorageService;
  private boolean forceReload = true;

  @PreAuthorize("hasRole('ROLE_SU')")
  public Map<String, Set<String>> getAllPermissionsReadonly() {
    reloadIfNeeded();
    Map<String, Set<String>> copy = new LinkedHashMap<>();
    currentPermissions.forEach(
        (String project, Set<String> emails) ->
            copy.put(project, Collections.unmodifiableSet(emails)));
    return Collections.unmodifiableMap(copy);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void grantEmailToProject(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    // we might add validation for project existence?
    Set<String> currentEmails = currentPermissions.getOrDefault(project, new HashSet<>());
    currentEmails.add(email);
    currentPermissions.put(project, currentEmails);
    save();
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void revokeEmailFromProject(String email, String project) {
    Objects.requireNonNull(email);
    Objects.requireNonNull(project);
    reloadIfNeeded();

    Set<String> currentEmails = currentPermissions.getOrDefault(project, new HashSet<>());
    currentEmails.remove(email);
    currentPermissions.put(project, currentEmails);
    save();
  }

  public List<String> getGrantsForEmail(String email) {
    reloadIfNeeded();

    List<String> grantsForEmail = new ArrayList<>();
    currentPermissions.forEach(
        (String projectName, Set<String> emailAddresses) -> {
          if (emailAddresses.contains(email)) {
            grantsForEmail.add(projectName);
          }
        });
    return grantsForEmail;
  }

  public synchronized void save() {
    String json = new Gson().toJson(currentPermissions);
    InputStream inputStream = new ByteArrayInputStream(json.getBytes());
    armadilloStorageService.saveSystemFile(
        inputStream, PERMISSIONS_FILE, MediaType.APPLICATION_JSON);
    forceReload = true;
  }

  public void reloadIfNeeded() {
    if (forceReload) {
      InputStream inputStream = armadilloStorageService.loadSystemFile(PERMISSIONS_FILE);

      Map<String, List<String>> temp =
          new Gson().fromJson(new InputStreamReader(inputStream), Map.class);
      if (temp == null) {
        currentPermissions = new LinkedHashMap<>();
      } else {
        currentPermissions = new LinkedHashMap<>();
        temp.forEach(
            (project, emailSet) -> currentPermissions.put(project, new HashSet<>(emailSet)));
      }

      forceReload = false;
    }
  }
}
