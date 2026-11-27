package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.ArmadilloServiceApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;

class ConfigKeysRegressionTest {

  private static final Pattern PLACEHOLDER_KEY = Pattern.compile("\\$\\{([^}:]+)");

  private final KnownConfigKeys known = KnownConfigKeys.loadFromClasspath();

  @Test
  void bundledApplicationYmlHasNoUnknownKeys() throws IOException {
    assertEquals(
        List.of(), checkerFor("src/main/resources/application.yml").findConfigKeyProblems());
  }

  @Test
  void applicationTemplateHasNoUnknownKeys() throws IOException {
    assertEquals(List.of(), checkerFor("../application.template.yml").findConfigKeyProblems());
  }

  @Test
  void everyKeyTheCodeReadsIsListedInMetadata() throws Exception {
    Set<String> unlisted = new TreeSet<>();
    for (Class<?> type : findMainClasses()) {
      for (String key : findKeysRead(type)) {
        if (!known.contains(key)) {
          unlisted.add(key + " (read in " + type.getSimpleName() + ")");
        }
      }
    }
    assertEquals(
        Set.of(),
        unlisted,
        "Add these keys to src/main/resources/META-INF/additional-spring-configuration-metadata.json");
  }

  private static ConfigKeyChecker checkerFor(String path) throws IOException {
    return ConfigKeyCheckerTest.checkerFor(new FileSystemResource(path));
  }

  private static List<Class<?>> findMainClasses() throws Exception {
    Path root =
        Path.of(
            ArmadilloServiceApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    List<Class<?>> classes = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
        String className = toClassName(root.relativize(file));
        if (!className.equals("module-info")) {
          classes.add(
              Class.forName(className, false, ConfigKeysRegressionTest.class.getClassLoader()));
        }
      }
    }
    return classes;
  }

  private static String toClassName(Path classFile) {
    String path = classFile.toString();
    return path.substring(0, path.length() - ".class".length())
        .replace(classFile.getFileSystem().getSeparator(), ".");
  }

  private static Set<String> findKeysRead(Class<?> type) {
    Set<String> keys = new TreeSet<>();
    for (AnnotatedElement element : findAnnotatedElements(type)) {
      Value value = element.getAnnotation(Value.class);
      if (value != null) {
        keys.addAll(findPlaceholderKeys(value.value()));
      }
      ConditionalOnProperty condition = element.getAnnotation(ConditionalOnProperty.class);
      if (condition != null) {
        keys.addAll(findConditionKeys(condition));
      }
    }
    return keys;
  }

  private static List<AnnotatedElement> findAnnotatedElements(Class<?> type) {
    List<AnnotatedElement> elements = new ArrayList<>(List.of(type));
    elements.addAll(Arrays.asList(type.getDeclaredFields()));
    List<Executable> executables =
        Stream.<Executable>concat(
                Arrays.stream(type.getDeclaredMethods()),
                Arrays.stream(type.getDeclaredConstructors()))
            .toList();
    for (Executable executable : executables) {
      elements.add(executable);
      elements.addAll(Arrays.asList(executable.getParameters()));
    }
    return elements;
  }

  private static Set<String> findPlaceholderKeys(String expression) {
    Set<String> keys = new TreeSet<>();
    Matcher matcher = PLACEHOLDER_KEY.matcher(expression);
    while (matcher.find()) {
      keys.add(matcher.group(1));
    }
    return keys;
  }

  private static Set<String> findConditionKeys(ConditionalOnProperty condition) {
    String prefix = condition.prefix().isEmpty() ? "" : condition.prefix() + ".";
    return Stream.concat(Arrays.stream(condition.value()), Arrays.stream(condition.name()))
        .map(name -> prefix + name)
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
