package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.molgenis.armadillo.exceptions.DuplicateObjectException;
import org.molgenis.armadillo.exceptions.DuplicateProjectException;
import org.molgenis.armadillo.exceptions.UnknownObjectException;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.molgenis.armadillo.model.Workspace;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ArmadilloStorageService {

  public static final String SHARED_PREFIX = "shared-";
  public static final String USER_PREFIX = "user-";
  public static final String BUCKET_REGEX = "(?=^.{3,63}$)(?!xn--)([a-z0-9][a-z0-9-]*[a-z0-9])";
  public static final String PARQUET = ".parquet";
  public static final String RDS = ".rds";
  private final StorageService storageService;

  public ArmadilloStorageService(StorageService storageService) {
    this.storageService = storageService;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void createProject(String project) {
    if (hasProject(project)) {
      throw new DuplicateProjectException(project);
    }

    storageService.createProjectIfNotExists(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public boolean hasProject(String project) {
    return storageService.listProjects().contains(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void deleteProject(String project) {
    if (!hasProject(project)) {
      throw new UnknownProjectException(project);
    }

    storageService.deleteProject(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void addObject(String project, String object, InputStream inputStream) {
    if (hasObject(project, object)) {
      throw new DuplicateObjectException(project, object);
    }

    storageService.save(inputStream, SHARED_PREFIX + project, object, APPLICATION_OCTET_STREAM);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public boolean hasObject(String project, String object) {
    if (!hasProject(project)) {
      throw new UnknownProjectException(project);
    }

    return storageService.listObjects(SHARED_PREFIX + project).stream()
        .anyMatch(objectMetadata -> objectMetadata.name().equals(object));
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void moveObject(String project, String newObject, String oldObject) {
    copyObject(project, newObject, oldObject);
    storageService.delete(SHARED_PREFIX + project, oldObject);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void copyObject(String project, String newObject, String oldObject) {
    if (!hasObject(project, oldObject)) {
      throw new UnknownObjectException(project, oldObject);
    }

    if (hasObject(project, newObject)) {
      throw new DuplicateObjectException(project, newObject);
    }

    var inputStream = storageService.load(SHARED_PREFIX + project, oldObject);
    storageService.save(inputStream, SHARED_PREFIX + project, newObject, APPLICATION_OCTET_STREAM);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void deleteObject(String project, String object) {
    if (!hasObject(project, object)) {
      throw new UnknownObjectException(project, object);
    }

    storageService.delete(SHARED_PREFIX + project, object);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public InputStream loadObject(String project, String object) {
    if (!hasObject(project, object)) {
      throw new UnknownObjectException(project, object);
    }

    return storageService.load(SHARED_PREFIX + project, object);
  }

  @PostFilter("hasAnyRole('ROLE_SU', 'ROLE_' + filterObject.toUpperCase() + '_RESEARCHER')")
  @SuppressWarnings("java:S6204") // result of method can't be unmodifiable because of @PostFilter
  public List<String> listProjects() {
    return storageService.listProjects().stream()
        .filter(it -> it.startsWith(SHARED_PREFIX))
        .map(it -> it.substring(SHARED_PREFIX.length()))
        .collect(toList());
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listObjects(String project) {
    if (!hasProject(project)) {
      throw new UnknownProjectException(project);
    }

    var projectName = SHARED_PREFIX + project;
    return storageService.listObjects(projectName).stream()
        .map(objectMetadata -> format("%s/%s", project, objectMetadata.name()))
        .toList();
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listTables(String project) {
    return listObjects(project).stream()
        .filter(it -> it.endsWith(PARQUET))
        .map(FilenameUtils::removeExtension)
        .toList();
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public boolean tableExists(String project, String objectName) {
    return storageService.objectExists(SHARED_PREFIX + project, objectName + PARQUET);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public InputStream loadTable(String project, String objectName) {
    return storageService.load(SHARED_PREFIX + project, objectName + PARQUET);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public boolean resourceExists(String project, String objectName) {
    return storageService.objectExists(SHARED_PREFIX + project, objectName + RDS);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public InputStream loadResource(String project, String objectName) {
    return storageService.load(SHARED_PREFIX + project, objectName + RDS);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listResources(String project) {
    return listObjects(project).stream()
        .filter(it -> it.endsWith(RDS))
        .map(FilenameUtils::removeExtension)
        .toList();
  }

  public List<Workspace> listWorkspaces(Principal principal) {
    return Stream.of(principal)
        .map(ArmadilloStorageService::getUserBucketName)
        .map(storageService::listObjects)
        .flatMap(List::stream)
        .map(ArmadilloStorageService::toWorkspace)
        .toList();
  }

  public InputStream loadWorkspace(Principal principal, String id) {
    return storageService.load(getUserBucketName(principal), getWorkspaceObjectName(id));
  }

  private static String getWorkspaceObjectName(String id) {
    return id + ".RData";
  }

  private static String getUserBucketName(Principal principal) {
    String bucketName = USER_PREFIX + principal.getName();
    if (!bucketName.matches(BUCKET_REGEX)) {
      throw new IllegalArgumentException(
          "Cannot create valid S3 bucket for username " + principal.getName());
    }
    return bucketName;
  }

  private static Workspace toWorkspace(ObjectMetadata item) {
    return Workspace.builder()
        .setLastModified(item.lastModified())
        .setName(removeExtension(item.name()))
        .setSize(item.size())
        .build();
  }

  public void saveWorkspace(InputStream is, Principal principal, String id) {
    storageService.save(
        is, getUserBucketName(principal), getWorkspaceObjectName(id), APPLICATION_OCTET_STREAM);
  }

  public void removeWorkspace(Principal principal, String id) {
    storageService.delete(getUserBucketName(principal), getWorkspaceObjectName(id));
  }
}
