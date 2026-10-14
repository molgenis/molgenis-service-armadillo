package org.molgenis.armadillo.metadata;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.molgenis.armadillo.exceptions.UnknownUserException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link AccessService}. The only mocked collaborators are {@link
 * ArmadilloStorageService} and {@link AccessLoader} — everything else (UserDetails, ProjectDetails,
 * ProjectPermission, AccessMetadata, RequestData) is a real AutoValue instance, since they're
 * simple immutable value objects and mocking them would add nothing.
 */
class AccessServiceTest {

  private ArmadilloStorageService storage;
  private AccessLoader loader;

  // in-memory "database" backing the loader mock, so save()/load() behave consistently
  // across the multiple internal save() calls AccessService makes per public method
  private AccessMetadata backingStore;

  @BeforeEach
  void setUp() {
    storage = mock(ArmadilloStorageService.class);
    loader = mock(AccessLoader.class);

    backingStore = AccessMetadata.create();

    when(loader.load()).thenAnswer(inv -> backingStore);
    when(loader.save(org.mockito.ArgumentMatchers.any(AccessMetadata.class)))
        .thenAnswer(
            inv -> {
              backingStore = inv.getArgument(0);
              return backingStore;
            });
    when(storage.listProjects()).thenReturn(emptyList());
  }

  private AccessService newService(String adminUser) {
    return new AccessService(storage, loader, adminUser);
  }

  private AccessService newServiceWithOidc(String adminUser, boolean oidcEnabled) {
    AccessService service = newService(adminUser);
    ReflectionTestUtils.setField(service, "oidcPermissionsEnabled", oidcEnabled);
    return service;
  }

  private static ConcurrentMap<String, UserDetails> concurrentUsers(UserDetails... users) {
    ConcurrentMap<String, UserDetails> map = new ConcurrentHashMap<>();
    for (UserDetails u : users) {
      map.put(u.getEmail(), u);
    }
    return map;
  }

  private static ConcurrentMap<String, ProjectDetails> concurrentProjects(
      ProjectDetails... projects) {
    ConcurrentMap<String, ProjectDetails> map = new ConcurrentHashMap<>();
    for (ProjectDetails p : projects) {
      map.put(p.getName(), p);
    }
    return map;
  }

  @Nested
  class Bootstrap {

    @Test
    void constructorCreatesAdminUserWhenConfiguredAndAbsent() {
      AccessService service = newService("admin@example.com");

      UserDetails admin = service.userByEmail("admin@example.com");

      assertThat(admin.getFirstName()).isNull();
      assertThat(service.usersList()).hasSize(1);
    }

    @Test
    void constructorDoesNotDuplicateExistingAdminUser() {
      UserDetails existingAdmin = UserDetails.createAdmin("admin@example.com");
      backingStore =
          AccessMetadata.create(
              concurrentUsers(existingAdmin), new ConcurrentHashMap<>(), new HashSet<>());

      AccessService service = newService("admin@example.com");

      assertThat(service.usersList()).hasSize(1);
    }

    @Test
    void constructorSkipsAdminCreationWhenAdminUserIsNull() {
      AccessService service = newService(null);

      assertThat(service.usersList()).isEmpty();
    }

    @Test
    void constructorImportsProjectsAlreadyPresentInStorage() {
      when(storage.listProjects()).thenReturn(List.of("project1", "project2"));

      AccessService service = newService(null);

      assertThat(service.projectsList())
          .extracting(ProjectDetails::getName)
          .containsExactlyInAnyOrder("project1", "project2");
    }

    @Test
    void constructorDoesNotReimportProjectsAlreadyKnown() {
      ProjectDetails existing = ProjectDetails.create("project1", emptySet());
      backingStore =
          AccessMetadata.create(
              new ConcurrentHashMap<>(), concurrentProjects(existing), new HashSet<>());
      when(storage.listProjects()).thenReturn(List.of("project1"));

      AccessService service = newService(null);

      assertThat(service.projectsList()).hasSize(1);
      // upsertProject is only called for genuinely new projects during bootstrap
      verify(storage, never()).upsertProject("project1");
    }
  }

  @Nested
  class Authorities {

    @Test
    void returnsResearcherRoleForEachProjectPermission() {
      AccessService service = newServiceWithOidc(null, false);
      service.permissionsAdd("user@example.com", "project1");
      service.permissionsAdd("user@example.com", "project2");

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("user@example.com", Map.of());

      assertThat(authorities)
          .extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder("ROLE_PROJECT1_RESEARCHER", "ROLE_PROJECT2_RESEARCHER");
    }

    @Test
    void returnsSuRoleForSuperUser() {
      AccessService service = newServiceWithOidc(null, false);
      service.userUpsert(UserDetails.create("admin@example.com", null, null, null, true, null));

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("admin@example.com", Map.of());

      assertThat(authorities).extracting(GrantedAuthority::getAuthority).contains("ROLE_SU");
    }

    @Test
    void doesNotAddSuRoleForNonAdminUser() {
      AccessService service = newServiceWithOidc(null, false);
      service.userUpsert(UserDetails.create("plain@example.com", null, null, null, false, null));

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("plain@example.com", Map.of());

      assertThat(authorities).extracting(GrantedAuthority::getAuthority).doesNotContain("ROLE_SU");
    }

    @Test
    void doesNotAddSuRoleForUnknownUser() {
      AccessService service = newServiceWithOidc(null, false);

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("ghost@example.com", Map.of());

      assertThat(authorities).extracting(GrantedAuthority::getAuthority).doesNotContain("ROLE_SU");
    }

    @Test
    void ignoresClaimRolesWhenOidcPermissionsDisabled() {
      AccessService service = newServiceWithOidc(null, false);

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("user@example.com", Map.of("roles", List.of("special")));

      assertThat(authorities)
          .extracting(GrantedAuthority::getAuthority)
          .doesNotContain("ROLE_SPECIAL");
    }

    @Test
    void addsClaimRolesWhenOidcPermissionsEnabled() {
      AccessService service = newServiceWithOidc(null, true);

      Collection<GrantedAuthority> authorities =
          service.getAuthoritiesForEmail("user@example.com", Map.of("roles", List.of("special")));

      assertThat(authorities).extracting(GrantedAuthority::getAuthority).contains("ROLE_SPECIAL");
    }
  }

  @Nested
  class Users {

    @Test
    void userUpsertAddsNewUser() {
      AccessService service = newService(null);

      service.userUpsert(UserDetails.create("new@example.com", "New", "User", "Inst", false, null));

      UserDetails result = service.userByEmail("new@example.com");
      assertThat(result.getFirstName()).isEqualTo("New");
      assertThat(result.getLastName()).isEqualTo("User");
      assertThat(result.getInstitution()).isEqualTo("Inst");
    }

    @Test
    void userUpsertWithProjectsCreatesProjectsAndPermissions() {
      AccessService service = newService(null);

      service.userUpsert(
          UserDetails.create("new@example.com", null, null, null, false, Set.of("projectA")));

      verify(storage, times(1)).upsertProject("projectA");
      assertThat(service.projectsByName("projectA").getUsers()).contains("new@example.com");
      assertThat(service.userByEmail("new@example.com").getProjects()).contains("projectA");
    }

    @Test
    void userUpsertReplacesPreviousPermissionsForSameUser() {
      AccessService service = newService(null);
      service.userUpsert(
          UserDetails.create("u@example.com", null, null, null, false, Set.of("projectA")));

      service.userUpsert(
          UserDetails.create("u@example.com", null, null, null, false, Set.of("projectB")));

      UserDetails updated = service.userByEmail("u@example.com");
      assertThat(updated.getProjects()).containsExactly("projectB");
    }

    @Test
    void userDeleteRemovesUserAndPermissions() {
      AccessService service = newService(null);
      service.userUpsert(
          UserDetails.create("u@example.com", null, null, null, false, Set.of("projectA")));

      service.userDelete("u@example.com");

      assertThatThrownBy(() -> service.userByEmail("u@example.com"))
          .isInstanceOf(UnknownUserException.class);
      assertThat(service.projectsByName("projectA").getUsers()).doesNotContain("u@example.com");
    }

    @Test
    void userDeleteThrowsForUnknownUser() {
      AccessService service = newService(null);

      assertThatThrownBy(() -> service.userDelete("ghost@example.com"))
          .isInstanceOf(UnknownUserException.class);
    }

    @Test
    void userByEmailThrowsForUnknownUser() {
      AccessService service = newService(null);

      assertThatThrownBy(() -> service.userByEmail("ghost@example.com"))
          .isInstanceOf(UnknownUserException.class);
    }

    @Test
    void usersListReturnsAllUsers() {
      AccessService service = newService(null);
      service.userUpsert(UserDetails.create("a@example.com", null, null, null, false, null));
      service.userUpsert(UserDetails.create("b@example.com", null, null, null, false, null));

      assertThat(service.usersList())
          .extracting(UserDetails::getEmail)
          .containsExactlyInAnyOrder("a@example.com", "b@example.com");
    }
  }

  @Nested
  class Projects {

    @Test
    void projectsUpsertCreatesProjectInStorage() {
      AccessService service = newService(null);

      service.projectsUpsert(ProjectDetails.create("projectA", emptySet()));

      verify(storage, times(1)).upsertProject("projectA");
      assertThat(service.projectsByName("projectA")).isNotNull();
    }

    @Test
    void projectsUpsertWithUsersCreatesMissingUsersAndPermissions() {
      AccessService service = newService(null);

      service.projectsUpsert(ProjectDetails.create("projectA", Set.of("newuser@example.com")));

      assertThat(service.userByEmail("newuser@example.com")).isNotNull();
      assertThat(service.projectsByName("projectA").getUsers()).contains("newuser@example.com");
    }

    @Test
    void projectsUpsertReplacesPreviousPermissionsForSameProject() {
      AccessService service = newService(null);
      service.projectsUpsert(ProjectDetails.create("projectA", Set.of("u1@example.com")));

      service.projectsUpsert(ProjectDetails.create("projectA", Set.of("u2@example.com")));

      assertThat(service.projectsByName("projectA").getUsers()).containsExactly("u2@example.com");
    }

    @Test
    void projectsDeleteRemovesProjectAndPermissions() {
      AccessService service = newService(null);
      service.projectsUpsert(ProjectDetails.create("projectA", Set.of("u1@example.com")));

      service.projectsDelete("projectA");

      verify(storage, times(1)).deleteProject("projectA");
      assertThatThrownBy(() -> service.projectsByName("projectA"))
          .isInstanceOf(UnknownProjectException.class);
    }

    @Test
    void projectsByNameThrowsForUnknownProject() {
      AccessService service = newService(null);

      assertThatThrownBy(() -> service.projectsByName("ghost"))
          .isInstanceOf(UnknownProjectException.class);
    }

    @Test
    void projectsListReturnsAllProjects() {
      AccessService service = newService(null);
      service.projectsUpsert(ProjectDetails.create("projectA", emptySet()));
      service.projectsUpsert(ProjectDetails.create("projectB", emptySet()));

      assertThat(service.projectsList())
          .extracting(ProjectDetails::getName)
          .containsExactlyInAnyOrder("projectA", "projectB");
    }
  }

  @Nested
  class Permissions {

    @Test
    void permissionsAddCreatesUserAndProjectIfMissingAndAddsPermission() {
      AccessService service = newService(null);

      service.permissionsAdd("u@example.com", "projectA");

      assertThat(service.userByEmail("u@example.com")).isNotNull();
      assertThat(service.projectsByName("projectA")).isNotNull();
      assertThat(service.permissionsList())
          .extracting(ProjectPermission::getEmail)
          .contains("u@example.com");
    }

    @Test
    void permissionsDeleteRemovesOnlyTheGivenPair() {
      AccessService service = newService(null);
      service.permissionsAdd("u@example.com", "projectA");
      service.permissionsAdd("u@example.com", "projectB"); // shares email -> should survive
      service.permissionsAdd("u2@example.com", "projectA"); // shares project -> should survive

      service.permissionsDelete("u@example.com", "projectA");

      assertThat(service.permissionsList())
          .extracting(ProjectPermission::getEmail, ProjectPermission::getProject)
          .containsExactlyInAnyOrder(
              org.assertj.core.groups.Tuple.tuple("u@example.com", "projectB"),
              org.assertj.core.groups.Tuple.tuple("u2@example.com", "projectA"));
    }

    @Test
    void permissionsListReturnsAllPermissions() {
      AccessService service = newService(null);
      service.permissionsAdd("u1@example.com", "projectA");
      service.permissionsAdd("u2@example.com", "projectB");

      assertThat(service.permissionsList()).hasSize(2);
    }
  }

  @Test
  void settingsListAggregatesUsersProjectsAndPermissions() {
    AccessService service = newService(null);
    service.permissionsAdd("u@example.com", "projectA");

    AccessMetadata metadata = service.settingsList();

    assertThat(metadata.getUsers()).containsKey("u@example.com");
    assertThat(metadata.getProjects()).containsKey("projectA");
    assertThat(metadata.getPermissions())
        .extracting(ProjectPermission::getEmail)
        .contains("u@example.com");
  }

  @Nested
  class ApproveAccessRequest {

    @Test
    void throwsWhenProjectForRequestAlreadyExists() {
      AccessService service = newService(null);
      when(storage.hasProject("request1")).thenReturn(true);

      assertThatThrownBy(
              () -> service.approveAccessRequest("request1", new ArrayList<>(), "user@example.com"))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("already exists");

      verify(storage, never()).upsertProject("request1");
    }

    @Test
    void createsLinkedObjectsAndGrantsPermissionOnSuccess() throws Exception {
      AccessService service = newService(null);
      when(storage.hasProject("request1")).thenReturn(false);

      RequestData requestData =
          RequestData.create("sourceProject/folder/table1.parquet", "var1,var2");

      ArrayList<RequestData> requests = new ArrayList<>();
      requests.add(requestData);

      service.approveAccessRequest("request1", requests, "user@example.com");

      verify(storage, times(1)).upsertProject("request1");
      verify(storage, times(1))
          .createLinkedObject(
              eq("sourceProject"),
              eq("folder/table1"),
              eq("folder/table1"),
              eq("request1"),
              eq("var1,var2"));
      assertThat(service.permissionsList())
          .anyMatch(
              p -> p.getEmail().equals("user@example.com") && p.getProject().equals("request1"));
    }

    @Test
    void wrapsDownstreamFailurePerTableAsBadRequest() throws Exception {
      AccessService service = newService(null);
      when(storage.hasProject("request1")).thenReturn(false);
      org.mockito.Mockito.doThrow(new IOException("boom"))
          .when(storage)
          .createLinkedObject(anyString(), anyString(), anyString(), anyString(), anyString());

      RequestData requestData = RequestData.create("sourceProject/folder/table1", "var1");
      ArrayList<RequestData> requests = new ArrayList<>();
      requests.add(requestData);

      assertThatThrownBy(
              () -> service.approveAccessRequest("request1", requests, "user@example.com"))
          .isInstanceOf(ResponseStatusException.class)
          .hasMessageContaining("Cannot create and approve request");
    }

    @Test
    void throwsWhenTablePathHasWrongSegmentCount() {
      AccessService service = newService(null);
      when(storage.hasProject("request1")).thenReturn(false);

      RequestData requestData = RequestData.create("onlyOneSegment", "var1");
      ArrayList<RequestData> requests = new ArrayList<>();
      requests.add(requestData);

      assertThatThrownBy(
              () -> service.approveAccessRequest("request1", requests, "user@example.com"))
          .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void stripsParquetExtensionFromTableName() throws Exception {
      AccessService service = newService(null);
      when(storage.hasProject("request1")).thenReturn(false);

      RequestData requestData = RequestData.create("sourceProject/folder/table1.parquet", "var1");
      ArrayList<RequestData> requests = new ArrayList<>();
      requests.add(requestData);

      service.approveAccessRequest("request1", requests, "user@example.com");

      verify(storage)
          .createLinkedObject(
              anyString(), eq("folder/table1"), eq("folder/table1"), anyString(), anyString());
    }
  }
}
