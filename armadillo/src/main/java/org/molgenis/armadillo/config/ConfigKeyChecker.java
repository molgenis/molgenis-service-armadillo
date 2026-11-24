package org.molgenis.armadillo.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Warns at startup about keys in the loaded config files that nothing reads, for example because of
 * wrong indentation.
 */
@Component
class ConfigKeyChecker {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigKeyChecker.class);
  private static final String OAUTH2_CLIENT = "spring.security.oauth2.client";
  private static final String OAUTH2_RESOURCE_SERVER = "spring.security.oauth2.resourceserver";

  private final ConfigurableEnvironment environment;

  ConfigKeyChecker(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void warnAboutProblems() {
    findProblems().forEach(LOG::warn);
  }

  List<String> findProblems() {
    Set<String> problems = new LinkedHashSet<>();
    problems.addAll(runCheck("for unknown keys", this::findUnknownKeys));
    problems.addAll(
        runCheck(
            OAUTH2_CLIENT,
            () -> findUnusedKeysInside(OAUTH2_CLIENT, Bindable.of(OAuth2ClientProperties.class))));
    problems.addAll(
        runCheck(
            OAUTH2_RESOURCE_SERVER,
            () ->
                findUnusedKeysInside(
                    OAUTH2_RESOURCE_SERVER, Bindable.of(OAuth2ResourceServerProperties.class))));
    return new ArrayList<>(problems);
  }

  private List<String> runCheck(String description, Supplier<List<String>> check) {
    try {
      return check.get();
    } catch (RuntimeException e) {
      return List.of("Could not check %s: %s".formatted(description, e.getMessage()));
    }
  }

  private List<String> findUnknownKeys() {
    KnownConfigKeys knownKeys = KnownConfigKeys.loadFromClasspath();
    List<String> problems = new ArrayList<>();
    for (PropertySource<?> source : environment.getPropertySources()) {
      if (source instanceof OriginTrackedMapPropertySource fileSource) {
        for (String key : fileSource.getPropertyNames()) {
          if (!knownKeys.contains(key)) {
            problems.add(describeUnusedKey(key, fileSource.getOrigin(key)));
          }
        }
      }
    }
    return problems;
  }

  private <T> List<String> findUnusedKeysInside(String prefix, Bindable<T> target) {
    BindHandler handler =
        new NoUnboundElementsBindHandler(BindHandler.DEFAULT, ConfigKeyChecker::isConfigFile);
    try {
      Binder.get(environment).bind(prefix, target, handler);
      return List.of();
    } catch (BindException e) {
      if (e.getCause() instanceof UnboundConfigurationPropertiesException unused) {
        return unused.getUnboundProperties().stream()
            .map(property -> describeUnusedKey(property.getName().toString(), property.getOrigin()))
            .toList();
      }
      throw e;
    }
  }

  private static boolean isConfigFile(ConfigurationPropertySource source) {
    return source.getUnderlyingSource() instanceof OriginTrackedMapPropertySource;
  }

  private static String describeUnusedKey(String key, Origin origin) {
    return "Unknown configuration key '%s' (%s). It will be ignored, check its indentation and spelling."
        .formatted(key, Objects.toString(origin, "unknown location"));
  }
}
