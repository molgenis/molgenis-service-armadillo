package org.molgenis.armadillo.service;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.molgenis.armadillo.metadata.Vantage6Token;
import org.molgenis.armadillo.metadata.Vantage6TokensLoader;
import org.molgenis.armadillo.metadata.Vantage6TokensMetadata;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ROLE_SU')")
public class Vantage6TokenService {

  private final Vantage6TokensLoader loader;
  private Vantage6TokensMetadata metadata;

  public Vantage6TokenService(Vantage6TokensLoader loader) {
    this.loader = requireNonNull(loader);
  }

  @jakarta.annotation.PostConstruct
  public void init() {
    metadata = loader.load();
  }

  public List<Vantage6Token> getAll() {
    return List.copyOf(metadata.getTokens().values());
  }

  public Vantage6Token getById(String tokenId) {
    Vantage6Token token = metadata.getTokens().get(tokenId);
    if (token == null) {
      throw new IllegalArgumentException("Vantage6 token not found: " + tokenId);
    }
    return token;
  }

  public List<Vantage6Token> getByContainerName(String containerName) {
    return metadata.getTokens().values().stream()
        .filter(t -> containerName.equals(t.getContainerName()))
        .toList();
  }

  public Vantage6Token create(
      String containerName, Set<String> authorizedProjects, String description) {
    String id = UUID.randomUUID().toString();
    String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

    Vantage6Token token =
        Vantage6Token.create(id, containerName, authorizedProjects, createdAt, description);

    metadata.getTokens().put(id, token);
    save();
    return token;
  }

  public Vantage6Token updateProjects(String tokenId, Set<String> authorizedProjects) {
    Vantage6Token existing = getById(tokenId);

    Vantage6Token updated =
        Vantage6Token.create(
            existing.getId(),
            existing.getContainerName(),
            authorizedProjects,
            existing.getCreatedAt(),
            existing.getDescription());

    metadata.getTokens().put(tokenId, updated);
    save();
    return updated;
  }

  public void delete(String tokenId) {
    if (metadata.getTokens().remove(tokenId) == null) {
      throw new IllegalArgumentException("Vantage6 token not found: " + tokenId);
    }
    save();
  }

  public Set<String> getAuthorizedProjects(String tokenId) {
    return getById(tokenId).getAuthorizedProjects();
  }

  private void save() {
    metadata = loader.save(metadata);
  }
}
