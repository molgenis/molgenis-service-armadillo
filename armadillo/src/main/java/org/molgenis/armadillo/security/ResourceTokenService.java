package org.molgenis.armadillo.security;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for generating and validating temporary resource access tokens.
 *
 * <p>When a DataSHIELD session loads a resource, this service generates a short-lived token that
 * the R server can use to download the resource file. This allows resources to be downloaded within
 * a DataSHIELD context while blocking direct downloads by researchers.
 */
@Service
public class ResourceTokenService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTokenService.class);
  private static final long TOKEN_TTL_SECONDS = 60;
  private static final long CLEANUP_INTERVAL_SECONDS = 30;

  private final ConcurrentHashMap<String, ResourceTokenInfo> tokens = new ConcurrentHashMap<>();

  public ResourceTokenService() {
    // Schedule periodic cleanup of expired tokens
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        this::cleanupExpiredTokens,
        CLEANUP_INTERVAL_SECONDS,
        CLEANUP_INTERVAL_SECONDS,
        TimeUnit.SECONDS);
  }

  /** Information associated with a resource access token. */
  public record ResourceTokenInfo(
      String researcher, String project, String resource, Instant expiresAt) {}

  /**
   * Generate a new resource access token.
   *
   * @param researcher the researcher identity (for audit purposes)
   * @param project the project the resource belongs to
   * @param resource the resource path being accessed
   * @return a temporary bearer token
   */
  public String generateToken(String researcher, String project, String resource) {
    String token = UUID.randomUUID().toString();
    Instant expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);
    tokens.put(token, new ResourceTokenInfo(researcher, project, resource, expiresAt));
    LOGGER.debug(
        "Generated resource token for researcher={}, project={}, resource={}",
        researcher,
        project,
        resource);
    return token;
  }

  /**
   * Check if a token exists and is valid without consuming it.
   *
   * <p>This is used by the authentication filter to determine if a bearer token is a resource token
   * before letting the normal OAuth2 flow handle it.
   *
   * @param token the bearer token to check
   * @return the token info if valid, empty if invalid or expired
   */
  public Optional<ResourceTokenInfo> peek(String token) {
    ResourceTokenInfo info = tokens.get(token);
    if (info == null) {
      return Optional.empty();
    }
    if (Instant.now().isAfter(info.expiresAt())) {
      return Optional.empty();
    }
    return Optional.of(info);
  }

  /**
   * Validate and consume a resource access token.
   *
   * <p>Tokens are single-use and are removed upon successful validation.
   *
   * @param token the bearer token to validate
   * @return the token info if valid, empty if invalid or expired
   */
  public Optional<ResourceTokenInfo> validateAndConsume(String token) {
    ResourceTokenInfo info = tokens.remove(token);
    if (info == null) {
      LOGGER.debug("Token not found: {}", token);
      return Optional.empty();
    }
    if (Instant.now().isAfter(info.expiresAt())) {
      LOGGER.debug("Token expired: {}", token);
      return Optional.empty();
    }
    LOGGER.debug(
        "Token validated for researcher={}, project={}, resource={}",
        info.researcher(),
        info.project(),
        info.resource());
    return Optional.of(info);
  }

  private void cleanupExpiredTokens() {
    Instant now = Instant.now();
    tokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
  }
}
