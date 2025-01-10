package org.molgenis.armadillo.storage;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.removeExtension;
import static org.molgenis.armadillo.info.UserInformationRetriever.getUserIdentifierFromPrincipal;
import static org.molgenis.armadillo.storage.StorageService.getHumanReadableByteCount;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.molgenis.armadillo.exceptions.*;
import org.molgenis.armadillo.model.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ArmadilloStorageService {

  public static final String SHARED_PREFIX = "shared-";
  public static final String USER_PREFIX = "user-";
  public static final String BUCKET_REGEX = "(?=^.{3,63}$)(?!xn--)([a-z0-9][a-z0-9-]*[a-z0-9])";
  public static final String PARQUET = ".parquet";
  public static final String LINK_FILE = ".alf";
  public static final String RDS = ".rds";
  public static final String SYSTEM = "system";
  public static final String RDATA_EXT = ".RData";
  private final StorageService storageService;

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalStorageService.class);

  public ArmadilloStorageService(StorageService storageService) {
    this.storageService = storageService;
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void upsertProject(String project) {
    validateProjectName(project);
    storageService.createBucketIfNotExists(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public boolean hasProject(String project) {
    return storageService.listBuckets().contains(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void deleteProject(String project) {
    throwIfUnknown(project);
    storageService.deleteBucket(SHARED_PREFIX + project);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void addObject(String project, String object, InputStream inputStream) {
    throwIfDuplicate(project, object);
    storageService.save(inputStream, SHARED_PREFIX + project, object, APPLICATION_OCTET_STREAM);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public boolean hasObject(String project, String object) {
    throwIfUnknown(project);
    return storageService.objectExists(SHARED_PREFIX + project, object);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void moveObject(String project, String newObject, String oldObject) {
    copyObject(project, newObject, oldObject);
    storageService.delete(SHARED_PREFIX + project, oldObject);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void copyObject(String project, String newObject, String oldObject) {
    throwIfUnknown(project, oldObject);
    throwIfDuplicate(project, newObject);
    var inputStream = storageService.load(SHARED_PREFIX + project, oldObject);
    storageService.save(inputStream, SHARED_PREFIX + project, newObject, APPLICATION_OCTET_STREAM);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void createLinkedObject(
      String sourceProject,
      String sourceObject,
      String linkName,
      String linkProject,
      String variables)
      throws IOException, StorageException {
    throwIfUnknown(sourceProject, sourceObject + PARQUET);
    throwIfUnknown(linkProject);
    throwIfDuplicate(linkProject, linkName + LINK_FILE);
    // Save information in armadillo link file (alf)
    List<String> unavailableVariables =
        storageService.getUnavailableVariables(
            SHARED_PREFIX + sourceProject, sourceObject, variables);
    if (unavailableVariables.size() > 0) {
      throw new UnknownVariableException(
          sourceProject, sourceObject, unavailableVariables.toString());
    }
    try {
      ArmadilloLinkFile armadilloLinkFile =
          createLinkFileFromSource(sourceProject, sourceObject, variables, linkName, linkProject);
      InputStream is = armadilloLinkFile.toStream();
      storageService.save(
          is, SHARED_PREFIX + linkProject, armadilloLinkFile.getFileName(), APPLICATION_JSON);
    } catch (IllegalArgumentException e) {
      throw new InvalidObjectNameException(linkName);
    }
  }

  public ArmadilloLinkFile createArmadilloLinkFileFromStream(
      InputStream armadilloLinkFileStream, String project, String objectName) {
    return new ArmadilloLinkFile(armadilloLinkFileStream, project, objectName);
  }

  public ArmadilloLinkFile createLinkFileFromSource(
      String sourceProject,
      String sourceObject,
      String variables,
      String linkName,
      String linkProject) {
    return new ArmadilloLinkFile(sourceProject, sourceObject, variables, linkName, linkProject);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public void deleteObject(String project, String object) {
    throwIfUnknown(project, object);
    storageService.delete(SHARED_PREFIX + project, object);
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public InputStream loadObject(String project, String object) {
    throwIfUnknown(project, object);
    return storageService.load(SHARED_PREFIX + project, object);
  }

  @PostFilter("hasAnyRole('ROLE_SU', 'ROLE_' + filterObject.toUpperCase() + '_RESEARCHER')")
  @SuppressWarnings("java:S6204") // result of method can't be unmodifiable because of @PostFilter
  public List<String> listProjects() {
    return storageService.listBuckets().stream()
        .filter(it -> it.startsWith(SHARED_PREFIX))
        .map(it -> it.substring(SHARED_PREFIX.length()))
        .collect(toList());
  }

  @PreAuthorize("hasAnyRole('ROLE_SU', 'ROLE_' + #project.toUpperCase() + '_RESEARCHER')")
  public List<String> listObjects(String project) {
    throwIfUnknown(project);
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

  @PreAuthorize("hasAnyRole('ROLE_SU')")
  public Map<String, List<Workspace>> listAllUserWorkspaces() {
    List<String> availableUsers =
        storageService.listBuckets().stream()
            .filter((user) -> user.startsWith(USER_PREFIX))
            .toList();
    return availableUsers.stream()
        .collect(
            Collectors.toMap(
                userFolder -> userFolder,
                userFolder ->
                    storageService.listObjects(userFolder).stream()
                        .map(ArmadilloStorageService::toWorkspace)
                        .collect(Collectors.toList())));
  }

  public InputStream loadWorkspace(Principal principal, String id) {
    return storageService.load(getUserBucketName(principal), getWorkspaceObjectName(id));
  }

  static String getWorkspaceObjectName(String id) {
    return id + RDATA_EXT;
  }

  private static String getOldUserBucketName(Principal principal) {
    return USER_PREFIX + principal.getName();
  }

  static String getUserBucketName(Principal principal) {
    String userIdentifier = getUserIdentifierFromPrincipal(principal).replace("@", "__at__");
    return USER_PREFIX + userIdentifier;
  }

  private static Workspace toWorkspace(ObjectMetadata item) {
    return Workspace.builder()
        .setLastModified(item.lastModified())
        .setName(removeExtension(item.name()))
        .setSize(item.size())
        .build();
  }

  private void trySaveWorkspace(ArmadilloWorkspace workspace, Principal principal, String id) {
    try {
      storageService.save(
          workspace.createInputStream(),
          getUserBucketName(principal),
          getWorkspaceObjectName(id),
          APPLICATION_OCTET_STREAM);
    } catch (StorageException e) {
      throw new StorageException(e);
    }
  }

  public void moveWorkspacesIfInOldBucket(Principal principal) {
    String oldBucketName = getOldUserBucketName(principal);
    String newBucketName = getUserBucketName(principal);
    // only move workspaces from old bucket to new if there is no new bucket yet, we don't want to
    List<String> migrationStatus = new ArrayList<>();
    if (storageService.bucketExists(oldBucketName) && !storageService.bucketExists(newBucketName)) {
      LOGGER.info(
          "Found old workspaces bucket for user, moving workspaces from old directory [{}] to new directory [{}]",
          oldBucketName,
          newBucketName);
      List<ObjectMetadata> existingWorkspaces = storageService.listObjects(oldBucketName);
      existingWorkspaces.forEach(
          (ws) -> {
            String message = "";
            if (ws.name().toLowerCase().endsWith(RDATA_EXT.toLowerCase())) {
              try {
                storageService.moveWorkspace(ws, principal, oldBucketName, newBucketName);
                message =
                    format(
                        "Successfully migrated workspace [%s] from [%s] to [%s]",
                        ws.name(), oldBucketName, newBucketName);
              } catch (StorageException e) {
                message =
                    format(
                        "Can't migrate workspace [%s] from [%s] to [%s], because [%s]. Workspace needs to be moved manually.",
                        ws.name(), oldBucketName, newBucketName, e.getMessage());
              } finally {
                migrationStatus.add(message);
              }
            }
          });
      try {
        writeMigrationFile(migrationStatus, newBucketName);
      } catch (FileNotFoundException e) {
        LOGGER.warn("Can't write migration status file for user [{}].", newBucketName);
      }
    }
  }

  void writeMigrationFile(List<String> migrationStatus, String bucketName)
      throws FileNotFoundException {
    Path bucketPath =
        Paths.get(storageService.getRootDir(), bucketName).toAbsolutePath().normalize();
    Path path = Paths.get(bucketPath + "/migration-status.txt");
    try {
      Files.write(path, migrationStatus, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.warn("Cannot write migration file to [{}] because: [{}]", path, e.getMessage());
    }
  }

  public void saveWorkspace(InputStream is, Principal principal, String id) {
    // Load root dir
    File drive = new File("/");
    long usableSpace = drive.getUsableSpace();
    try {
      moveWorkspacesIfInOldBucket(principal);
      ArmadilloWorkspace workspace = storageService.getWorkSpace(is);

      long fileSize = workspace.getSize();
      if (usableSpace > fileSize * 2L) {
        trySaveWorkspace(workspace, principal, id);
      } else {
        throw new StorageException(
            format(
                "Can't save workspace: workspace too big (%s), not enough space left on device. Try to make your workspace smaller and/or contact the administrator to increase diskspace.",
                getHumanReadableByteCount(fileSize)));
      }
    } catch (StorageException e) {
      throw new StorageException(e.getMessage().replace("load", "save"));
    }
  }

  public void removeWorkspace(Principal principal, String id) {
    storageService.delete(getUserBucketName(principal), getWorkspaceObjectName(id));
  }

  public void removeWorkspaceByStringUserId(String userId, String id) {
    storageService.delete(USER_PREFIX + userId, getWorkspaceObjectName(id));
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

  private void throwIfDuplicate(String project, String object) {
    if (hasObject(project, object)) {
      throw new DuplicateObjectException(project, object);
    }
  }

  private void throwIfUnknown(String project) {
    if (!hasProject(project)) {
      throw new UnknownProjectException(project);
    }
  }

  private void throwIfUnknown(String project, String object) {
    if (!hasObject(project, object)) {
      throw new UnknownObjectException(project, object);
    }
  }

  static void validateProjectName(String projectName) {
    requireNonNull(projectName);

    Pattern pattern = Pattern.compile("(?!((^xn--)|(-s3alias$)))^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$");
    if (!pattern.matcher(projectName).matches()) {
      throw new InvalidProjectNameException(projectName);
    }
  }

  public long getFileSizeIfObjectExists(String bucketName, String objectName) throws IOException {
    Path filePath = storageService.getPathIfObjectExists(bucketName, objectName);
    return Files.size(filePath);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public List<Map<String, String>> getPreview(String project, String object) {
    throwIfUnknown(project, object);
    return storageService.preview(SHARED_PREFIX + project, object, 10, 10);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public List<String> getVariables(String project, String object) {
    throwIfUnknown(project, object);
    return storageService.getVariables(SHARED_PREFIX + project, object);
  }

  @PreAuthorize("hasRole('ROLE_SU')")
  public FileInfo getInfo(String project, String object) {
    throwIfUnknown(project, object);
    return storageService.getInfo(SHARED_PREFIX + project, object);
  }
}
