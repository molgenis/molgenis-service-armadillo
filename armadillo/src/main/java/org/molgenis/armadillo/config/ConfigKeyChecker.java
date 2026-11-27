package org.molgenis.armadillo.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.molgenis.armadillo.metadata.InitialProfileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

/**
 * Warns at startup about unrecognised keys in the loaded config files, for example because of wrong
 * indentation.
 */
@Component
class ConfigKeyChecker {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigKeyChecker.class);
  private static final String OAUTH2_CLIENT = "spring.security.oauth2.client";
  private static final String PROFILES = "armadillo.profiles";

  private final ConfigurableEnvironment environment;

  ConfigKeyChecker(ConfigurableEnvironment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void warnAboutConfigKeyProblems() {
    findConfigKeyProblems().forEach(LOG::warn);
  }

  List<String> findConfigKeyProblems() {
    Set<String> problems = new LinkedHashSet<>();
    problems.addAll(runCheck("for unknown keys", this::findKeysNotInKnownList));
    problems.addAll(runCheck(OAUTH2_CLIENT, this::findUnmatchedKeysInOidcClient));
    problems.addAll(runCheck(PROFILES, this::findUnmatchedKeysInProfiles));
    return new ArrayList<>(problems);
  }

  private List<String> runCheck(String description, Supplier<List<String>> check) {
    try {
      return check.get();
    } catch (RuntimeException e) {
      return List.of("Could not check %s: %s".formatted(description, e.getMessage()));
    }
  }

  private List<String> findKeysNotInKnownList() {
    KnownConfigKeys knownKeys = KnownConfigKeys.loadFromClasspath();
    return configFiles()
        .flatMap(
            file ->
                Arrays.stream(file.getPropertyNames())
                    .filter(key -> !knownKeys.contains(key))
                    .map(key -> describeUnknownKey(key, file.getOrigin(key))))
        .toList();
  }

  private List<String> findUnmatchedKeysInOidcClient() {
    return findUnmatchedKeysInside(
        Binder.get(environment), OAUTH2_CLIENT, Bindable.of(OAuth2ClientProperties.class));
  }

  // One file at a time: Spring takes a list from a single source and reports the rest as unmatched
  private List<String> findUnmatchedKeysInProfiles() {
    PropertySourcesPlaceholdersResolver resolver =
        new PropertySourcesPlaceholdersResolver(environment);
    return configFiles()
        .map(file -> new Binder(ConfigurationPropertySources.from(file), resolver))
        .flatMap(
            binder ->
                findUnmatchedKeysInside(
                    binder, PROFILES, Bindable.listOf(InitialProfileConfig.class))
                    .stream())
        .toList();
  }

  private <T> List<String> findUnmatchedKeysInside(
      Binder binder, String prefix, Bindable<T> target) {
    BindHandler handler =
        new NoUnboundElementsBindHandler(BindHandler.DEFAULT, ConfigKeyChecker::isConfigFile);
    try {
      binder.bind(prefix, target, handler);
      return List.of();
    } catch (BindException e) {
      if (e.getCause() instanceof UnboundConfigurationPropertiesException unmatched) {
        return unmatched.getUnboundProperties().stream()
            .map(
                property -> describeUnknownKey(property.getName().toString(), property.getOrigin()))
            .toList();
      }
      throw e;
    }
  }

  private Stream<OriginTrackedMapPropertySource> configFiles() {
    return environment.getPropertySources().stream()
        .filter(OriginTrackedMapPropertySource.class::isInstance)
        .map(OriginTrackedMapPropertySource.class::cast);
  }

  private static boolean isConfigFile(ConfigurationPropertySource source) {
    return source.getUnderlyingSource() instanceof OriginTrackedMapPropertySource;
  }

  private static String describeUnknownKey(String key, Origin origin) {
    return "Unknown configuration key '%s' (%s). It will be ignored, check its indentation and spelling."
        .formatted(key, Objects.toString(origin, "unknown location"));
  }
}
