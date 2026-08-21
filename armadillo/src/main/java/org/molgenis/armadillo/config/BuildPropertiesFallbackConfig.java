package org.molgenis.armadillo.config;

import java.util.Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(BuildProperties.class)
public class BuildPropertiesFallbackConfig {

  @Bean
  public BuildProperties buildProperties() {
    Properties props = new Properties();
    props.setProperty("version", "dev");
    return new BuildProperties(props);
  }
}
