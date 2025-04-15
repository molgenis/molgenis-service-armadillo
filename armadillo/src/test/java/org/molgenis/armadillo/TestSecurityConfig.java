package org.molgenis.armadillo;

import static java.util.Collections.emptyList;

import java.util.List;
import org.molgenis.armadillo.metadata.AccessLoader;
import org.molgenis.armadillo.metadata.AccessService;
import org.molgenis.armadillo.metadata.DummyAccessLoader;
import org.molgenis.armadillo.metadata.DummyProfilesLoader;
import org.molgenis.armadillo.metadata.InitialProfileConfigs;
import org.molgenis.armadillo.metadata.InsightService;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.metadata.ProfilesLoader;
import org.molgenis.armadillo.profile.ProfileScope;
import org.molgenis.armadillo.service.FileService;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {
  public TestSecurityConfig() {}

  @Bean
  protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers("/ds-profiles/status")
                    .permitAll()
                    .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .build();
  }

  @Bean
  AccessLoader accessLoader() {
    return new DummyAccessLoader();
  }

  @Bean
  ProfilesLoader profilesLoader() {
    return new DummyProfilesLoader();
  }

  @Bean
  ProfileScope profileScope() {
    return new ProfileScope();
  }

  @Bean
  AccessService accessService(ArmadilloStorageService storageService, AccessLoader accessLoader) {
    return new AccessService(storageService, accessLoader, null);
  }

  @Bean
  InsightService insightService(FileService fileService) {
    return new InsightService(fileService);
  }

  @Bean
  FileService fileService() {
    return new FileService();
  }

  @Bean
  ProfileService profileService(ProfilesLoader profilesLoader, ProfileScope profileScope) {
    var initialProfiles = new InitialProfileConfigs();
    initialProfiles.setProfiles(emptyList());

    return new ProfileService(profilesLoader, initialProfiles, profileScope);
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
