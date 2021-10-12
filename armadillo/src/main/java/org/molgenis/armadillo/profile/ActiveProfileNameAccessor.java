package org.molgenis.armadillo.profile;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_SESSION;
import static org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ActiveProfileNameAccessor {

  public static final String DEFAULT = "default";
  private static final String PROFILE_CONTEXT_KEY = "profile";

  private ActiveProfileNameAccessor() {}

  public static String getActiveProfileName() {
    try {
      Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
      synchronized (mutex) {
        return Optional.ofNullable(
                (String)
                    RequestContextHolder.currentRequestAttributes()
                        .getAttribute(PROFILE_CONTEXT_KEY, SCOPE_SESSION))
            .orElse(DEFAULT);
      }
    } catch (IllegalStateException e) {
      return DEFAULT;
    }
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
