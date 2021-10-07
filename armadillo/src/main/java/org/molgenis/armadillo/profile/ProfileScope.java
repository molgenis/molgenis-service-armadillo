package org.molgenis.armadillo.profile;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ProfileScope implements Scope {
  /** Contains all profile scoped beans for all profiles */
  private final ConcurrentHashMap<String, Object> scopedBeans = new ConcurrentHashMap<>();

  @Override
  public Object get(String beanName, ObjectFactory<?> objectFactory) {
    final var fullyQualifiedBeanName = getFullyQualifiedBeanName(beanName);
    return Optional.ofNullable(scopedBeans.get(fullyQualifiedBeanName))
        .orElseGet(() -> createProfileBean(objectFactory, fullyQualifiedBeanName));
  }

  private Object createProfileBean(ObjectFactory<?> objectFactory, String fullyQualifiedBeanName) {
    Object bean = objectFactory.getObject();
    scopedBeans.put(fullyQualifiedBeanName, bean);
    return bean;
  }

  @Override
  public Object remove(@NonNull String beanName) {
    return scopedBeans.remove(getFullyQualifiedBeanName(beanName));
  }

  @Override
  public void registerDestructionCallback(String beanName, Runnable runnable) {
    // We do not use this because our set of beans is application scoped
  }

  @Override
  public Object resolveContextualObject(String s) {
    if ("profileName".equals(s)) {
      return getConversationId();
    }
    return null;
  }

  @Override
  public String getConversationId() {
    return ActiveProfileNameAccessor.getActiveProfileName();
  }

  private String getFullyQualifiedBeanName(String beanName) {
    return ActiveProfileNameAccessor.getActiveProfileName() + "." + beanName;
  }
}
