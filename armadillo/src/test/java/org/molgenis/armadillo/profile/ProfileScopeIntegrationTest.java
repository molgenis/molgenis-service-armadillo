package org.molgenis.armadillo.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.command.CommandsConfig;
import org.molgenis.armadillo.config.annotation.ProfileScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(controllers = ProfileScopeTestController.class)
@ExtendWith(MockitoExtension.class)
class ProfileScopeIntegrationTest {

  @Autowired private MockMvc mockMvc;
  private final ExecutorService executors = Executors.newFixedThreadPool(10);

  @Configuration
  @Import({
    BeanA.class,
    CommandsConfig.class,
    ProfileConfig.class,
    org.molgenis.armadillo.profile.ProfileScope.class,
    ProfileScopeTestController.class
  })
  static class Config {
    @Bean
    @ProfileScope
    BeanB b() {
      return new BeanB(ActiveProfileNameAccessor.getActiveProfileName());
    }
  }

  @Test
  @WithMockUser
  void testGetProfile() throws Exception {
    for (int i = 0; i < 100; i++) {
      var profileName = "profile " + i % 10;
      var result =
          mockMvc.perform(get("/get-profile").sessionAttr("profile", profileName)).andReturn();
      mockMvc
          .perform(asyncDispatch(result))
          .andExpect(MockMvcResultMatchers.content().string(profileName));
    }
  }
}
