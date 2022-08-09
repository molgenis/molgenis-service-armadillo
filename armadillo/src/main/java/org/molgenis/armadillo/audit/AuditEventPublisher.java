package org.molgenis.armadillo.audit;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;

import java.security.Principal;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

@Component
public class AuditEventPublisher implements ApplicationEventPublisherAware {

  public static final String SELECT_PROFILE = "SELECT_PROFILE";
  public static final String PROFILES = "PROFILES";
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
  public static final String PERMISSIONS_LIST = "PERMISSIONS_LIST";
  public static final String PERMISSIONS_ADD = "PERMISSIONS_ADD";
  public static final String PERMISSIONS_DELETE = "PERMISSIONS_DELETE";
  public static final String UPSERT_USER = "UPSERT_USER";
  public static final String DELETE_USER = "DELETE_USER";
  public static final String GET_USER = "GET_USER";
  public static final String LIST_PROJECTS = "LIST_PROJECTS";
  public static final String UPSERT_PROJECT = "UPSERT_PROJECT";
  public static final String DELETE_PROJECT = "DELETE_PROJECT";
  public static final String GET_PROJECT = "GET_PROJECT";
  public static final String LIST_USERS = "LIST_USERS";
  public static final String GET_TABLES = "GET_TABLES";
  public static final String LOAD_TABLE = "LOAD_TABLE";
  public static final String GET_RESOURCES = "GET_RESOURCES";
  public static final String INSTALL_PACKAGES = "INSTALL_PACKAGES";
  public static final String INSTALL_PACKAGES_FAILURE = "INSTALL_PACKAGES_FAILURE";
  public static final String EXPRESSION = "expression";
  public static final String SELECTED_PROFILE = "selectedProfile";
  public static final String TYPE = "type";
  public static final String FOLDER = "folder";
  public static final String RESOURCE = "resource";
  public static final String SYMBOL = "symbol";
  public static final String PROJECT = "project";
  public static final String EMAIL = "email";
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

  public void audit(
      Principal principal,
      String type,
      Map<String, Object> data,
      String sessionId,
      List<String> roles) {
    Map<String, Object> sessionData = new HashMap<>(data);
    sessionData.put("sessionId", sessionId);
    sessionData.put("roles", roles);
    applicationEventPublisher.publishEvent(
        new AuditApplicationEvent(clock.instant(), principal.getName(), type, sessionData));
  }

  public void audit(Principal principal, String type, Map<String, Object> data) {
    audit(principal, type, data, MDC.get("sessionID"), getRoles());
  }

  private static List<String> getRoles() {
    return Optional.ofNullable(getContext()).map(SecurityContext::getAuthentication).stream()
        .map(Authentication::getAuthorities)
        .flatMap(Collection::stream)
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  public <T> CompletableFuture<T> audit(
      CompletableFuture<T> future, Principal principal, String type, Map<String, Object> data) {
    // remember context to fill it in when future completes
    final var sessionId = MDC.get("sessionID");
    final var roles = getRoles();
    return future.whenComplete(
        (success, failure) -> {
          if (failure == null) {
            audit(principal, type, data, sessionId, roles);
          } else {
            Map<String, Object> errorData = new HashMap<>(data);
            errorData.put(MESSAGE, failure.getMessage());
            errorData.put(TYPE, failure.getClass().getName());
            audit(principal, type + "_FAILURE", errorData, sessionId, roles);
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
