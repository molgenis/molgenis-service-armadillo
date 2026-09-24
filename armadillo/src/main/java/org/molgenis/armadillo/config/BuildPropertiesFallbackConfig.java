package org.molgenis.armadillo.config;

import java.util.Properties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@Conditional(BuildPropertiesFallbackConfig.BuildInfoMissing.class)
public class BuildPropertiesFallbackConfig {

  @Bean
  public BuildProperties buildProperties() {
    Properties props = new Properties();
    props.setProperty("version", "dev");
    return new BuildProperties(props);
  }

  static class BuildInfoMissing implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !context
          .getResourceLoader()
          .getResource("classpath:META-INF/build-info.properties")
          .exists();
    }
  }
}
