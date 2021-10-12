package org.molgenis.armadillo.profile;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;

import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;

public class ActiveProfileNameAccessor {

  public static final String DEFAULT = "default";
  private static final String PROFILE_CONTEXT_KEY = "profile";

  private static final ThreadLocal<String> ACTIVE_PROFILE = ThreadLocal.withInitial(() -> DEFAULT);

  private ActiveProfileNameAccessor() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  /**
   * Sets the active profile name in the user session or in the current thread if no
   * RequestAttributes object is bound to the current thread.
   *
   * @param activeProfileName the profile name to select
   * @throws IllegalStateException if no RequestAttributes object is bound to the current thread
   */
  public static void setActiveProfileName(String activeProfileName) {
    Optional.ofNullable(getRequestAttributes())
        .ifPresentOrElse(
            requestAttributes ->
                requestAttributes.setAttribute(
                    PROFILE_CONTEXT_KEY, activeProfileName, SCOPE_SESSION),
            () -> ACTIVE_PROFILE.set(activeProfileName));
  }

  /**
   * Retrieves the active profile name, either from the user session if a RequestAttributes object
   * is bound to the current thread, or the value bound to the current thread.
   */
  public static String getActiveProfileName() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(it -> (String) it.getAttribute(PROFILE_CONTEXT_KEY, SCOPE_SESSION))
        .orElseGet(ACTIVE_PROFILE::get);
  }

  public static void resetActiveProfileName() {
    ACTIVE_PROFILE.remove();
  }
}
