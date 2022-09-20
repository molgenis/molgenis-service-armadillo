package org.molgenis.armadillo.security;

import static org.springframework.security.core.context.SecurityContextHolder.createEmptyContext;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.security.core.context.SecurityContextHolder.setContext;

import java.util.function.Supplier;

public class RunAs {

  private RunAs() {}

  /** Run a single function with elevated permissions (ROLE_SU). */
  public static <T> T runAsSystem(Supplier<T> runnable) {
    var originalContext = getContext();
    try {
      setContext(createEmptyContext());
      getContext().setAuthentication(new SystemSecurityToken());

      return runnable.get();
    } finally {
      setContext(originalContext);
    }
  }

  /** Run a single void function with elevated permissions (ROLE_SU). */
  public static void runAsSystem(Runnable runnable) {
    runAsSystem(
        () -> {
          runnable.run();
          return null;
        });
  }
}
