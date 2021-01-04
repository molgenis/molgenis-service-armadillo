package org.molgenis.armadillo.audit;

import java.security.Principal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

@Component
public class AuditEventPublisher implements ApplicationEventPublisherAware {

  private ApplicationEventPublisher applicationEventPublisher;
  private Clock clock = Clock.systemUTC();

  public void setClock(Clock clock) {
    this.clock = clock;
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void audit(Principal principal, String type, Map<String, Object> data, String sessionId) {
    Map<String, Object> sessionData = new HashMap<>(data);
    sessionData.put("sessionId", sessionId);
    applicationEventPublisher.publishEvent(
        new AuditApplicationEvent(clock.instant(), principal.getName(), type, sessionData));
  }

  public void audit(Principal principal, String type, Map<String, Object> data) {
    audit(principal, type, data, MDC.get("sessionID"));
  }

  public <T> CompletableFuture<T> audit(
      CompletableFuture<T> c, Principal principal, String type, Map<String, Object> data) {
    String sessionId = MDC.get("sessionID");
    return c.whenComplete(
        (success, failure) -> {
          if (failure == null) {
            audit(principal, type, data, sessionId);
          } else {
            Map<String, Object> errorData = new HashMap<>(data);
            errorData.put("message", failure.getMessage());
            errorData.put("type", failure.getClass().getSimpleName());
            audit(principal, type + "_FAILURE", errorData, sessionId);
          }
        });
  }

  public <T> T audit(Supplier<T> c, Principal principal, String type, Map<String, Object> data) {
    try {
      var result = c.get();
      audit(principal, type, data);
      return result;
    } catch (Throwable failure) {
      Map<String, Object> errorData = new HashMap<>(data);
      errorData.put("message", failure.getMessage());
      errorData.put("type", failure.getClass().getSimpleName());
      audit(principal, type + "_FAILURE", errorData);
      throw failure;
    }
  }
}
