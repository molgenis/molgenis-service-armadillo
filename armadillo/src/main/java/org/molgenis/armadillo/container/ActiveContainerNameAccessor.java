package org.molgenis.armadillo.container;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;

import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;

public class ActiveContainerNameAccessor {

  public static final String DEFAULT = "default";
  private static final String PROFILE_CONTEXT_KEY = "container";

  private static final ThreadLocal<String> ACTIVE_PROFILE = ThreadLocal.withInitial(() -> DEFAULT);

  private ActiveContainerNameAccessor() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  /**
   * RequestAttributes object is bound to the current thread.
   *
   * @throws IllegalStateException if no RequestAttributes object is bound to the current thread
   */
  public static void setActiveContainerName(String activeProfileName) {
    Optional.ofNullable(getRequestAttributes())
        .ifPresentOrElse(
            requestAttributes ->
                requestAttributes.setAttribute(
                    PROFILE_CONTEXT_KEY, activeProfileName, SCOPE_SESSION),
            () -> ACTIVE_PROFILE.set(activeProfileName));
  }

  /** is bound to the current thread, or the value bound to the current thread. */
  public static String getActiveContainerName() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(it -> (String) it.getAttribute(PROFILE_CONTEXT_KEY, SCOPE_SESSION))
        .orElseGet(ACTIVE_PROFILE::get);
  }

  public static void resetActiveContainerName() {
    ACTIVE_PROFILE.remove();
  }
}
