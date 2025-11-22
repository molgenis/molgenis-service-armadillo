package org.molgenis.armadillo.profile.annotation;

import java.lang.annotation.*;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope("profile")
public @interface ContainerScope {

  /**
   * Alias for {@link Scope#proxyMode}.
   *
   * <p>Defaults to {@link ScopedProxyMode#TARGET_CLASS}.
   */
  @AliasFor(annotation = Scope.class)
  ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}
