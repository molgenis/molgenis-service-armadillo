package org.molgenis.armadillo.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.service.DataShieldProfileEnvironments;
import org.molgenis.r.RConnectionFactory;

@ExtendWith(MockitoExtension.class)
class ProfileTest {
  ProfileConfigProps profileConfigProps = new ProfileConfigProps();

  @Mock RConnectionFactory rConnectionFactory;
  @Mock DataShieldProfileEnvironments dataShieldProfileEnvironments;
  @Mock DataShieldOptions dataShieldOptions;

  Profile profile;

  @BeforeEach
  void setup() {
    profileConfigProps.setEnvironment("environment");
    profileConfigProps.setName("default");
    profileConfigProps.setWhitelist(Set.of("dsBase", "dsExposome"));
    profileConfigProps.setOptions(Map.of("foo", "bar"));
    profile =
        new Profile(
            profileConfigProps,
            rConnectionFactory,
            dataShieldProfileEnvironments,
            dataShieldOptions);
  }

  @Test
  void testInit() {
    profile.init();

    verify(dataShieldProfileEnvironments).populateEnvironments();
    verify(dataShieldOptions).init();
  }

  @Test
  void testGetProfileName() {
    assertEquals("default", profile.getProfileName());
  }
}
