package org.molgenis.armadillo.minio;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import io.minio.messages.Bucket;
import io.minio.messages.Item;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.molgenis.armadillo.model.Workspace;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ArmadilloStorageService {
  public static final String SHARED_PREFIX = "shared-";
  public static final String USER_PREFIX = "user-";
  public static final String BUCKET_REGEX = "(?=^.{3,63}$)(?!xn--)([a-z0-9](?:[a-z0-9-]*)[a-z0-9])";
  public static final String PARQUET = ".parquet";
  public static final String RDS = ".rds";
  public static final String SYSTEM = "system";
  private final MinioStorageService storageService;

  public ArmadilloStorageService(MinioStorageService storageService) {
    this.storageService = storageService;
  }

  @PostFilter("hasAnyRole('ROLE_SU', 'ROLE_' + filterObject.toUpperCase() + '_RESEARCHER')")
  public List<String> listProjects() {
    return storageService.listBuckets().stream()
        .map(Bucket::name)
        .filter(it -> it.startsWith(SHARED_PREFIX))
        .map(it -> it.substring(SHARED_PREFIX.length()))
        .collect(toList());
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listTables(String project) {
    var bucketName = SHARED_PREFIX + project;
    return storageService.listObjects(bucketName).stream()
        .map(Item::objectName)
        .map(objectName -> format("%s/%s", project, objectName))
        .filter(it -> it.endsWith(PARQUET))
        .map(FilenameUtils::removeExtension)
        .collect(toList());
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
        .map(Item::objectName)
        .map(objectName -> format("%s/%s", project, objectName))
        .filter(it -> it.endsWith(RDS))
        .map(FilenameUtils::removeExtension)
        .collect(toList());
  }

  public List<Workspace> listWorkspaces(Principal principal) {
    return Stream.of(principal)
        .map(ArmadilloStorageService::getUserBucketName)
        .map(storageService::listObjects)
        .flatMap(List::stream)
        .map(ArmadilloStorageService::toWorkspace)
        .collect(toList());
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

  private static Workspace toWorkspace(Item item) {
    return Workspace.builder()
        .setLastModified(item.lastModified())
        .setName(removeExtension(item.objectName()))
        .setSize(item.objectSize())
        .setETag(item.etag())
        .build();
  }

  public void saveWorkspace(InputStream is, Principal principal, String id) {
    storageService.save(
        is, getUserBucketName(principal), getWorkspaceObjectName(id), APPLICATION_OCTET_STREAM);
  }

  public void removeWorkspace(Principal principal, String id) {
    storageService.delete(getUserBucketName(principal), getWorkspaceObjectName(id));
  }

  public void saveSystemFile(InputStream is, String name, MediaType mediaType) {
    storageService.save(is, SYSTEM, name, mediaType);
  }

  public InputStream loadSystemFile(String name) {
    if (storageService.objectExists(SYSTEM, name)) {
      return storageService.load(SYSTEM, name);
    } else {
      return InputStream.nullInputStream();
    }
  }
}
