package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.molgenis.armadillo.exceptions.UnknownVantage6TokenException;
import org.molgenis.armadillo.metadata.Vantage6Token;
import org.molgenis.armadillo.metadata.Vantage6TokensLoader;
import org.molgenis.armadillo.metadata.Vantage6TokensMetadata;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Vantage6TokenServiceTest {

  @Mock private Vantage6TokensLoader loader;

  private Vantage6TokenService tokenService;

  @BeforeEach
  void setup() {
    Vantage6TokensMetadata metadata = Vantage6TokensMetadata.create();
    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    tokenService = new Vantage6TokenService(loader);
    tokenService.init();
  }

  @Test
  void testCreateToken() {
    Set<String> projects = Set.of("shared-projectA", "shared-projectB");
    Vantage6Token token = tokenService.create("v6-node", projects, "test token");

    assertNotNull(token.getId());
    assertEquals("v6-node", token.getContainerName());
    assertEquals(projects, token.getAuthorizedProjects());
    assertEquals("test token", token.getDescription());
    assertNotNull(token.getCreatedAt());
  }

  @Test
  void testGetById() {
    Vantage6Token created = tokenService.create("v6-node", Set.of("proj1"), "desc");

    Vantage6Token fetched = tokenService.getById(created.getId());

    assertEquals(created.getId(), fetched.getId());
    assertEquals("v6-node", fetched.getContainerName());
  }

  @Test
  void testGetByIdNotFound() {
    assertThrows(UnknownVantage6TokenException.class, () -> tokenService.getById("nonexistent"));
  }

  @Test
  void testGetByContainerName() {
    tokenService.create("v6-node-1", Set.of("projA"), "first");
    tokenService.create("v6-node-2", Set.of("projB"), "second");
    tokenService.create("v6-node-1", Set.of("projC"), "third");

    var tokens = tokenService.getByContainerName("v6-node-1");

    assertEquals(2, tokens.size());
    assertTrue(tokens.stream().allMatch(t -> "v6-node-1".equals(t.getContainerName())));
  }

  @Test
  void testUpdateProjects() {
    Vantage6Token created = tokenService.create("v6-node", Set.of("proj1"), "desc");

    Set<String> newProjects = Set.of("proj2", "proj3");
    Vantage6Token updated = tokenService.updateProjects(created.getId(), newProjects);

    assertEquals(created.getId(), updated.getId());
    assertEquals(newProjects, updated.getAuthorizedProjects());
    assertEquals("v6-node", updated.getContainerName());
    assertEquals("desc", updated.getDescription());
  }

  @Test
  void testDelete() {
    Vantage6Token created = tokenService.create("v6-node", Set.of("proj1"), "desc");
    String id = created.getId();

    tokenService.delete(id);

    assertThrows(UnknownVantage6TokenException.class, () -> tokenService.getById(id));
  }

  @Test
  void testDeleteNotFound() {
    assertThrows(UnknownVantage6TokenException.class, () -> tokenService.delete("nonexistent"));
  }

  @Test
  void testGetAuthorizedProjects() {
    Set<String> projects = Set.of("projA", "projB");
    Vantage6Token created = tokenService.create("v6-node", projects, "desc");

    Set<String> result = tokenService.getAuthorizedProjects(created.getId());

    assertEquals(projects, result);
  }

  @Test
  void testGetAll() {
    assertEquals(0, tokenService.getAll().size());

    tokenService.create("v6-1", Set.of("p1"), "first");
    tokenService.create("v6-2", Set.of("p2"), "second");

    assertEquals(2, tokenService.getAll().size());
  }
}
