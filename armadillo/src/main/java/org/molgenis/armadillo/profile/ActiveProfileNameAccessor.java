package org.molgenis.armadillo.profile;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;

import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;

public class ActiveProfileNameAccessor {

  public static final String DEFAULT = "default";
  private static final String PROFILE_CONTEXT_KEY = "profile";

  private static final ThreadLocal<String> ACTIVE_PROFILE = ThreadLocal.withInitial(() -> DEFAULT);

  private ActiveProfileNameAccessor() {}

  public static String getActiveProfileName() {
    return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(it -> (String) it.getAttribute(PROFILE_CONTEXT_KEY, SCOPE_SESSION))
        .orElseGet(ACTIVE_PROFILE::get);
  }

  public static void setActiveProfileThreadLocal(String activeProfileName) {
    ACTIVE_PROFILE.set(activeProfileName);
  }

  public static void resetActiveProfileThreadLocal() {
    ACTIVE_PROFILE.remove();
  }

  public static void setActiveProfileName(String activeProfileName) {
    Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
    synchronized (mutex) {
      currentRequestAttributes().removeAttribute("scopedTarget.ArmadilloSession", SCOPE_SESSION);
      currentRequestAttributes()
          .setAttribute(PROFILE_CONTEXT_KEY, activeProfileName, SCOPE_SESSION);
    }
  }
}
