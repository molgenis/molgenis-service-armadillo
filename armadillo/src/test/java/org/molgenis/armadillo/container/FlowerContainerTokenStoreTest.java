package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlowerContainerTokenStoreTest {

  private final FlowerContainerTokenStore store = new FlowerContainerTokenStore();

  @Test
  void mint_producesDifferentTokensForDifferentContainers() {
    String tokenA = store.mint("container-a");
    String tokenB = store.mint("container-b");

    assertNotEquals(tokenA, tokenB);
  }

  @Test
  void mint_resolvesBackToTheContainerItWasMintedFor() {
    String token = store.mint("container-a");

    assertEquals("container-a", store.resolve(token).orElseThrow());
  }

  @Test
  void mint_reMintingInvalidatesThePreviousToken() {
    String oldToken = store.mint("container-a");
    String newToken = store.mint("container-a");

    assertTrue(store.resolve(oldToken).isEmpty());
    assertEquals("container-a", store.resolve(newToken).orElseThrow());
  }

  @Test
  void resolve_returnsEmptyForUnmintedToken() {
    assertTrue(store.resolve("never-minted").isEmpty());
  }

  @Test
  void resolve_returnsEmptyForNullToken() {
    assertTrue(store.resolve(null).isEmpty());
  }

  @Test
  void remove_thenResolveReturnsEmpty() {
    String token = store.mint("container-a");

    store.remove("container-a");

    assertTrue(store.resolve(token).isEmpty());
  }

  @Test
  void remove_isNoOpWhenNothingWasMinted() {
    store.remove("never-minted-container");
  }
}
