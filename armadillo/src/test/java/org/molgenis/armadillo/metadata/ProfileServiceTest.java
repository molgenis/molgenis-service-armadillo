package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.profile.ProfileScope;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private ProfileScope profileScope;

  @Mock private InitialProfileConfigs initialProfileConfigs;

  @Test
  void addToWhitelist() {
    var profilesMetadata = ProfilesMetadata.create();
    var defaultProfile =
        ProfileConfig.create(
            "default", "test", "localhost", 1234, new HashSet<>(), Set.of(), new HashMap<>());
    profilesMetadata.getProfiles().put("default", defaultProfile);
    var profilesLoader = new DummyProfilesLoader(profilesMetadata);
    var profileService = new ProfileService(profilesLoader, initialProfileConfigs, profileScope);

    profileService.initialize();

    profileService.addToWhitelist("default", "dsOmics");

    verify(profileScope).removeAllProfileBeans("default");
    assertTrue(profileService.getByName("default").getPackageWhitelist().contains("dsOmics"));
  }
}
