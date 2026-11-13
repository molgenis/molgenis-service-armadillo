package org.molgenis.armadillo.container;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;

import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;

public class ActiveContainerNameAccessor {

  public static final String DEFAULT = "default";
  private static final String CONTAINER_CONTEXT_KEY = "container";

  private static final ThreadLocal<String> ACTIVE_CONTAINER =
      ThreadLocal.withInitial(() -> DEFAULT);

  private ActiveContainerNameAccessor() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  /**
   * Sets the active container name in the user session or in the current thread if no
   * RequestAttributes object is bound to the current thread.
   *
   * @param activeContainerName the container name to select
   * @throws IllegalStateException if no RequestAttributes object is bound to the current thread
   */
  public static void setActiveContainerName(String activeContainerName) {
    Optional.ofNullable(getRequestAttributes())
        .ifPresentOrElse(
            requestAttributes ->
                requestAttributes.setAttribute(
                    CONTAINER_CONTEXT_KEY, activeContainerName, SCOPE_SESSION),
            () -> ACTIVE_CONTAINER.set(activeContainerName));
  }

  /**
   * Retrieves the active container name, either from the user session if a RequestAttributes object
   * is bound to the current thread, or the value bound to the current thread.
   */
  public static String getActiveContainerName() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(it -> (String) it.getAttribute(CONTAINER_CONTEXT_KEY, SCOPE_SESSION))
        .orElseGet(ACTIVE_CONTAINER::get);
  }

  public static void resetActiveContainerName() {
    ACTIVE_CONTAINER.remove();
  }
}
