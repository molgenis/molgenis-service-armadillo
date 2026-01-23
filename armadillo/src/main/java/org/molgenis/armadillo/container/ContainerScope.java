package org.molgenis.armadillo.container;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ContainerScope implements Scope {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContainerScope.class);

  private final ConcurrentHashMap<String, Object> scopedBeans = new ConcurrentHashMap<>();

  @Override
  public Object get(String beanName, ObjectFactory<?> objectFactory) {
    return scopedBeans.computeIfAbsent(
        getFullyQualifiedBeanName(beanName),
        name -> {
          LOGGER.info("Creating container bean with name {}", name);
          return objectFactory.getObject();
        });
  }

  @Override
  public Object remove(@NonNull String beanName) {
    return scopedBeans.remove(getFullyQualifiedBeanName(beanName));
  }

  public void removeAllContainerBeans(String containerName) {
    scopedBeans
        .keys()
        .asIterator()
        .forEachRemaining(
            key -> {
              if (key.startsWith(containerName)) {
                scopedBeans.remove(key);
              }
            });
  }

  @Override
  public void registerDestructionCallback(String beanName, Runnable runnable) {
    // We do not use this because our set of beans is application scoped
  }

  @Override
  public Object resolveContextualObject(String s) {
    return null;
  }

  @Override
  public String getConversationId() {
    return null;
  }

  private String getFullyQualifiedBeanName(String beanName) {
    return ActiveContainerNameAccessor.getActiveContainerName() + "." + beanName;
  }
}
