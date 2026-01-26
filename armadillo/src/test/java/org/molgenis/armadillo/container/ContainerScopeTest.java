package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;

class ContainerScopeTest {

  @AfterEach
  void resetActiveContainerName() {
    ActiveContainerNameAccessor.resetActiveContainerName();
  }

  @Test
  void get_returnsSameInstancePerContainer() {
    ContainerScope scope = new ContainerScope();
    ObjectFactory<Object> factory = Object::new;

    ActiveContainerNameAccessor.setActiveContainerName("c1");
    Object first = scope.get("bean", factory);
    Object second = scope.get("bean", factory);

    assertSame(first, second);

    ActiveContainerNameAccessor.setActiveContainerName("c2");
    Object third = scope.get("bean", factory);

    assertNotSame(first, third);
  }

  @Test
  void remove_removesBeanForCurrentContainer() {
    ContainerScope scope = new ContainerScope();
    ObjectFactory<Object> factory = Object::new;

    ActiveContainerNameAccessor.setActiveContainerName("c1");
    Object first = scope.get("bean", factory);
    Object removed = scope.remove("bean");

    assertSame(first, removed);
    Object newInstance = scope.get("bean", factory);
    assertNotSame(first, newInstance);
  }

  @Test
  void removeAllContainerBeans_removesMatchingPrefix() {
    ContainerScope scope = new ContainerScope();
    ObjectFactory<Object> factory = Object::new;

    ActiveContainerNameAccessor.setActiveContainerName("c1");
    Object first = scope.get("bean", factory);

    ActiveContainerNameAccessor.setActiveContainerName("c2");
    scope.get("bean", factory);

    scope.removeAllContainerBeans("c1");

    ActiveContainerNameAccessor.setActiveContainerName("c1");
    Object after = scope.get("bean", factory);

    assertNotSame(first, after);
  }

  @Test
  void registerDestructionCallback_noop() {
    ContainerScope scope = new ContainerScope();
    scope.registerDestructionCallback("bean", () -> {});
  }

  @Test
  void resolveContextualObject_returnsNull() {
    ContainerScope scope = new ContainerScope();
    assertNull(scope.resolveContextualObject("anything"));
  }

  @Test
  void getConversationId_returnsNull() {
    ContainerScope scope = new ContainerScope();
    assertNull(scope.getConversationId());
  }
}
