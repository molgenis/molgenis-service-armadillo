package org.molgenis.armadillo.config;

import org.molgenis.armadillo.service.PythonRebootScriptRunner;
import org.molgenis.armadillo.service.RebootScriptRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  RebootScriptRunner rebootScriptRunner() {
    return new PythonRebootScriptRunner();
  }
}
