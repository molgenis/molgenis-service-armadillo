package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultContainerFactoryConfigTest {

  @Test
  void defaultContainerFactory_selectsMatchingTypeIgnoringCase() {
    DefaultContainerFactoryConfig config = new DefaultContainerFactoryConfig("DeFaUlT");
    DefaultContainerFactory expected = new StubFactory("default");

    DefaultContainerFactory result =
        config.defaultContainerFactory(List.of(new StubFactory("ds"), expected));

    assertSame(expected, result);
  }

  @Test
  void defaultContainerFactory_throwsWhenTypeMissing() {
    DefaultContainerFactoryConfig config = new DefaultContainerFactoryConfig("missing");
    List<DefaultContainerFactory> factories = List.of(new StubFactory("ds"));

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> config.defaultContainerFactory(factories));

    assertEquals("No DefaultContainerFactory registered for type 'missing'.", ex.getMessage());
  }

  private static final class StubFactory implements DefaultContainerFactory {
    private final String type;

    private StubFactory(String type) {
      this.type = type;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public ContainerConfig createDefault() {
      throw new UnsupportedOperationException("not needed for test");
    }
  }
}
