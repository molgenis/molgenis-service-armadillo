package org.molgenis.armadillo;

import com.github.dockerjava.api.DockerClient;
import java.util.List;
import org.molgenis.armadillo.metadata.AccessLoader;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.DummyAccessLoader;
import org.molgenis.armadillo.profile.DockerService;
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
  public DockerService armadilloProfileService(DockerClient dockerClient) {
    return new DockerService(dockerClient, true);
  }

  @Bean
  AccessLoader metadataLoader() {
    return new DummyAccessLoader();
  }

  @Bean
  AccessService metadataService(ArmadilloStorageService storageService, AccessLoader accessLoader) {
    return new AccessService(storageService, accessLoader, null);
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
    return new InMemoryUserDetailsManager(List.of(userDetails));
  }
}
