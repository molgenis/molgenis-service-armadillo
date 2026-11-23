package org.molgenis.armadillo.config;

import static java.lang.String.format;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import org.molgenis.armadillo.service.DefaultRebootScriptRunner;
import org.molgenis.armadillo.service.RebootScriptRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  private static final String DEV = "DEV";

  @Bean
  RebootScriptRunner rebootScriptRunner(
      @Value("${stdout.log.path:./logs/armadillo.log}") String configuredLogPath) {
    return new DefaultRebootScriptRunner(configuredLogPath);
  }

  @Bean
  String armadilloMode(@Value("${armadillo.armadillo-mode:PROD}") String armadilloMode) {
    return armadilloMode;
  }

  @Bean
  String jarHome(
      @Value("${armadillo.armadillo-mode:PROD}") String armadilloMode,
      @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}") String armadilloHome) {
    if (DEV.equals(armadilloMode)) {
      return format("%s/build/libs", armadilloHome);
    } else {
      return format("%s", armadilloHome);
    }
  }

  @Bean
  public HttpClient httpClient() {
    return HttpClient.newBuilder().proxy(ProxySelector.getDefault()).build();
  }

  @Bean
  ConfigFile configUpdater(
      @Value("${armadillo.armadillo-config-file:/etc/armadillo/application.yml}")
          String armadilloConfigFile) {
    return new ApplicationConfigFile(armadilloConfigFile);
  }
}
