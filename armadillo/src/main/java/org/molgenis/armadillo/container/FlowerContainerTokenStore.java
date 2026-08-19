package org.molgenis.armadillo.container;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Holds a random, in-memory-only secret per running Flower clientapp container, keyed both ways
 * (container name to token, and token back to container name). Minted at container start
 * (DockerService#configureEnv) and used to resolve a push-data caller's identity from the token it
 * presents (FlowerController#pushData), instead of trusting a self-reported container name.
 * Deliberately never persisted: not exposed via any GET /containers response, and rotates
 * automatically on every container (re)start.
 */
@Component
public class FlowerContainerTokenStore {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();
  private final ConcurrentHashMap<String, String> tokensByContainerName = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> containerNamesByToken = new ConcurrentHashMap<>();

  /** Mints (or re-mints, invalidating any previous token) a token for containerName. */
  public String mint(String containerName) {
    String previousToken = tokensByContainerName.get(containerName);
    if (previousToken != null) {
      containerNamesByToken.remove(previousToken);
    }

    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    tokensByContainerName.put(containerName, token);
    containerNamesByToken.put(token, containerName);
    return token;
  }

  /** Resolves the container name a token was minted for, if any. */
  public Optional<String> resolve(String token) {
    return Optional.ofNullable(token).map(containerNamesByToken::get);
  }

  /** Removes any token minted for containerName. No-op if none exists. */
  public void remove(String containerName) {
    String token = tokensByContainerName.remove(containerName);
    if (token != null) {
      containerNamesByToken.remove(token);
    }
  }
}
