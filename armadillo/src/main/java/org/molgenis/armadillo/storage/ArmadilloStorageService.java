package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
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

  @PostFilter("hasAnyRole('ROLE_SU', 'ROLE_' + filterObject.toUpperCase() + '_RESEARCHER')")
  @SuppressWarnings("java:S6204") // result of method can't be unmodifiable because of @PostFilter
  public List<String> listProjects() {
    return storageService.listProjects().stream()
        .filter(it -> it.startsWith(SHARED_PREFIX))
        .map(it -> it.substring(SHARED_PREFIX.length()))
        .collect(Collectors.toList());
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listTables(String project) {
    var bucketName = SHARED_PREFIX + project;
    return storageService.listObjects(bucketName).stream()
        .map(objectMetadata -> format("%s/%s", project, objectMetadata.name()))
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
    var bucketName = SHARED_PREFIX + project;
    return storageService.listObjects(bucketName).stream()
        .map(objectMetadata -> format("%s/%s", project, objectMetadata.name()))
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
