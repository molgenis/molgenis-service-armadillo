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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
  public static final String LIST_ACCESS_DATA = "LIST_ACCESS_DATA";
  public static final String LIST_PROJECTS = "LIST_PROJECTS";
  public static final String UPSERT_PROJECT = "UPSERT_PROJECT";
  public static final String DELETE_PROJECT = "DELETE_PROJECT";
  public static final String GET_PROJECT = "GET_PROJECT";
  public static final String LIST_PROFILES = "LIST_PROFILES";
  public static final String UPSERT_PROFILE = "UPSERT_PROFILE";
  public static final String DELETE_PROFILE = "DELETE_PROFILE";
  public static final String GET_PROFILE = "GET_PROFILE";
  public static final String START_PROFILE = "START_PROFILE";
  public static final String STOP_PROFILE = "STOP_PROFILE";
  public static final String LIST_OBJECTS = "LIST_OBJECTS";
  public static final String UPLOAD_OBJECT = "UPLOAD_OBJECT";
  public static final String COPY_OBJECT = "COPY_OBJECT";
  public static final String MOVE_OBJECT = "MOVE_OBJECT";
  public static final String GET_OBJECT = "GET_OBJECT";
  public static final String PREVIEW_OBJECT = "PREVIEW_OBJECT";
  public static final String GET_OBJECT_INFO = "GET_OBJECT_INFO";
  public static final String DELETE_OBJECT = "DELETE_OBJECT";
  public static final String DOWNLOAD_OBJECT = "DOWNLOAD_OBJECT";
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
  public static final String PROFILE = "profile";
  public static final String OBJECT = "object";
  public static final String EMAIL = "email";
  public static final String MESSAGE = "message";
  public static final String TABLE = "table";
  public static final String ID = "id";
  static final String ANONYMOUS = "ANONYMOUS";
  public static final String MDC_SESSION_ID = "sessionID";
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
    var user = getUser(principal);
    applicationEventPublisher.publishEvent(
        new AuditApplicationEvent(clock.instant(), user, type, sessionData));
  }

  static String getUser(Object principal) {
    if (principal == null) {
      return ANONYMOUS;
    } else if (principal instanceof OAuth2AuthenticationToken token) {
      return token.getPrincipal().getAttribute(EMAIL);
    } else if (principal instanceof JwtAuthenticationToken token) {
      return token.getTokenAttributes().get(EMAIL).toString();
    } else if (principal instanceof DefaultOAuth2User user) {
      return user.getAttributes().get(EMAIL).toString();
    } else if (principal instanceof Jwt jwt) {
      return jwt.getClaims().get(EMAIL).toString();
    } else if (principal instanceof User user) {
      return user.getUsername();
    } else if (principal instanceof Principal p) {
      return p.getName();
    } else {
      return principal.toString();
    }
  }

  public void audit(Principal principal, String type, Map<String, Object> data) {
    audit(principal, type, data, MDC.get(MDC_SESSION_ID), getRoles());
  }

  /** Audits a CompletableFuture. */
  public <T> CompletableFuture<T> audit(
      CompletableFuture<T> future, Principal principal, String type, Map<String, Object> data) {
    // remember context to fill it in when future completes
    final var sessionId = MDC.get(MDC_SESSION_ID);
    final var roles = getRoles();

    return future.whenComplete(
        (success, failure) -> {
          if (failure == null) {
            audit(principal, type, data, sessionId, roles);
          } else {
            auditFailure(principal, type, data, failure, sessionId, roles);
          }
        });
  }

  /** Audits a function with a return value. */
  public <T> T audit(Supplier<T> c, Principal principal, String type, Map<String, Object> data) {
    try {
      var result = c.get();
      audit(principal, type, data);
      return result;
    } catch (Exception failure) {
      auditFailure(principal, type, data, failure);
      throw failure;
    }
  }

  /** Audits a function with a return value. */
  public <T> T audit(Supplier<T> c, Principal principal, String type) {
    return audit(c, principal, type, Map.of());
  }

  /** Audits a void function. */
  public void audit(Runnable runnable, Principal principal, String type, Map<String, Object> data) {
    try {
      runnable.run();
      audit(principal, type, data);
    } catch (Exception failure) {
      auditFailure(principal, type, data, failure);
      throw failure;
    }
  }

  /** Audits a void function. */
  public void audit(Runnable runnable, Principal principal, String type) {
    audit(runnable, principal, type, Map.of());
  }

  private void auditFailure(
      Principal principal, String type, Map<String, Object> data, Throwable failure) {
    auditFailure(principal, type, data, failure, MDC.get(MDC_SESSION_ID), getRoles());
  }

  private void auditFailure(
      Principal principal,
      String type,
      Map<String, Object> data,
      Throwable failure,
      String sessionId,
      List<String> roles) {
    Map<String, Object> errorData = new HashMap<>(data);
    errorData.put(MESSAGE, failure.getMessage());
    errorData.put(TYPE, failure.getClass().getName());
    audit(principal, type + "_FAILURE", errorData, sessionId, roles);
  }

  private static List<String> getRoles() {
    return Optional.ofNullable(getContext()).map(SecurityContext::getAuthentication).stream()
        .map(Authentication::getAuthorities)
        .flatMap(Collection::stream)
        .map(GrantedAuthority::getAuthority)
        .toList();
  }
}
