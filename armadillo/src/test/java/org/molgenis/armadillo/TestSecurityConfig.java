package org.molgenis.armadillo;

import com.github.dockerjava.api.DockerClient;
import java.util.Arrays;
import java.util.List;
import org.molgenis.armadillo.config.ArmadilloProfileService;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Configuration
public class TestSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().anyRequest().authenticated().and().httpBasic().and().csrf().disable();
  }

  @Bean
  public ArmadilloProfileService armadilloProfileService(DockerClient dockerClient) {
    return new ArmadilloProfileService(dockerClient);
  }

  @Bean
  ArmadilloMetadataService accessStorageService(
      ArmadilloStorageService storageService, ArmadilloProfileService profileService) {
    return new ArmadilloMetadataService(storageService, profileService);
  }

  @Bean
  // used in SettingsController test
  public UserDetailsService userDetailsService() {
    GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SU");
    User userDetails =
        new User("bofke", "bofke", List.of(authority)) {
          public String getEmail() {
            return "bofke@email.com";
          }
        };
    return new InMemoryUserDetailsManager(Arrays.asList(userDetails));
  }
}
