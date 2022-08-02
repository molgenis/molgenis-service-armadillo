package org.molgenis.armadillo;

import org.molgenis.armadillo.settings.ArmadilloSettingsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().anyRequest().authenticated().and().httpBasic().and().csrf().disable();
  }

  @Bean
  ArmadilloSettingsService accessStorageService() {
    return new ArmadilloSettingsService();
  }
}
