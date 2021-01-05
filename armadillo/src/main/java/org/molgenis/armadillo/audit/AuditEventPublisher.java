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

  public static final String GET_ASSIGNED_SYMBOLS = "GET_ASSIGNED_SYMBOLS";
  public static final String GET_PACKAGES = "GET_PACKAGES";
  public static final String LOAD_TABLE_FAILURE = "LOAD_TABLE_FAILURE";
  public static final String RESOURCE_EXISTS = "RESOURCE_EXISTS";
  public static final String TABLE_EXISTS = "TABLE_EXISTS";
  public static final String LOAD_RESOURCE = "LOAD_RESOURCE";
  public static final String LOAD_RESOURCE_FAILURE = "LOAD_RESOURCE_FAILURE";
  public static final String REMOVE_SYMBOL = "REMOVE_SYMBOL";
  public static final String ASSIGN1 = "ASSIGN";
  public static final String ASSIGN_FAILURE = "ASSIGN_FAILURE";
  public static final String EXECUTE = "EXECUTE";
  public static final String EXECUTE_FAILURE = "EXECUTE_FAILURE";
  public static final String DEBUG = "DEBUG";
  public static final String GET_ASSIGN_METHODS = "GET_ASSIGN_METHODS";
  public static final String GET_AGGREGATE_METHODS = "GET_AGGREGATE_METHODS";
  public static final String GET_USER_WORKSPACES = "GET_USER_WORKSPACES";
  public static final String DELETE_USER_WORKSPACE = "DELETE_USER_WORKSPACE";
  public static final String SAVE_USER_WORKSPACE = "SAVE_USER_WORKSPACE";
  public static final String LOAD_USER_WORKSPACE = "LOAD_USER_WORKSPACE";
  public static final String GET_TABLES = "GET_TABLES";
  public static final String LOAD_TABLE = "LOAD_TABLE";
  public static final String GET_RESOURCES = "GET_RESOURCES";
  public static final String EXPRESSION = "expression";
  public static final String TYPE = "type";
  public static final String FOLDER = "folder";
  public static final String RESOURCE = "resource";
  public static final String SYMBOL = "symbol";
  public static final String PROJECT = "project";
  public static final String MESSAGE = "message";
  public static final String TABLE = "table";
  public static final String ID = "id";
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
            errorData.put(MESSAGE, failure.getMessage());
            errorData.put(TYPE, failure.getClass().getName());
            audit(principal, type + "_FAILURE", errorData, sessionId);
          }
        });
  }

  public <T> T audit(Supplier<T> c, Principal principal, String type, Map<String, Object> data) {
    try {
      var result = c.get();
      audit(principal, type, data);
      return result;
    } catch (Exception failure) {
      Map<String, Object> errorData = new HashMap<>(data);
      errorData.put(MESSAGE, failure.getMessage());
      errorData.put(TYPE, failure.getClass().getName());
      audit(principal, type + "_FAILURE", errorData);
      throw failure;
    }
  }
}
