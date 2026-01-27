package org.molgenis.armadillo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.molgenis.armadillo.security.ResourceTokenService.ResourceTokenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that authenticates requests using resource tokens.
 *
 * <p>When a DataSHIELD session loads a resource, the R server needs to download the resource data.
 * It uses a temporary bearer token (not a JWT) for authentication. This filter intercepts those
 * requests and authenticates them before the OAuth2 JWT filter can reject them.
 */
public class ResourceTokenAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ResourceTokenAuthenticationFilter.class);
  private static final Pattern OBJECT_DOWNLOAD_PATTERN =
      Pattern.compile("^/storage/projects/([^/]+)/objects/(.+)$");
  private static final String BEARER_PREFIX = "Bearer ";

  private final ResourceTokenService resourceTokenService;

  public ResourceTokenAuthenticationFilter(ResourceTokenService resourceTokenService) {
    this.resourceTokenService = resourceTokenService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Only process GET requests to object download endpoint
    if (!"GET".equals(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();
    Matcher matcher = OBJECT_DOWNLOAD_PATTERN.matcher(path);
    if (!matcher.matches()) {
      filterChain.doFilter(request, response);
      return;
    }

    String project = matcher.group(1);
    String object = java.net.URLDecoder.decode(matcher.group(2), "UTF-8");

    // Check for Bearer token
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    String bearerToken = authHeader.substring(BEARER_PREFIX.length());

    // Try to validate as resource token (peek without consuming)
    Optional<ResourceTokenInfo> tokenInfo = resourceTokenService.peek(bearerToken);
    if (tokenInfo.isEmpty()) {
      // Not a resource token, let OAuth2 handle it
      filterChain.doFilter(request, response);
      return;
    }

    ResourceTokenInfo info = tokenInfo.get();

    // Validate token is for this project and is a resource file
    if (!info.project().equals(project) || !isResourceFile(object)) {
      LOGGER.debug(
          "Resource token mismatch: expected project={}, got={}; isResource={}",
          info.project(),
          project,
          isResourceFile(object));
      filterChain.doFilter(request, response);
      return;
    }

    // Valid resource token - create authentication
    LOGGER.debug("Authenticating request with resource token for researcher={}", info.researcher());
    ResourceTokenAuthentication authentication = new ResourceTokenAuthentication(info);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  private boolean isResourceFile(String object) {
    return object.endsWith(".rds") || object.endsWith(".rda");
  }

  /** Authentication token for resource token-based access. */
  public static class ResourceTokenAuthentication extends AbstractAuthenticationToken {

    private final ResourceTokenInfo tokenInfo;

    public ResourceTokenAuthentication(ResourceTokenInfo tokenInfo) {
      super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_RESOURCE_DOWNLOAD")));
      this.tokenInfo = tokenInfo;
      setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return tokenInfo.researcher();
    }

    @Override
    public String getName() {
      return tokenInfo.researcher();
    }

    public ResourceTokenInfo getTokenInfo() {
      return tokenInfo;
    }
  }
}
