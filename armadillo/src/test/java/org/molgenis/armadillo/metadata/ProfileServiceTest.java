package org.molgenis.armadillo.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
            "default", "test", "localhost", 1234, new HashSet<>(), Set.of(), new HashMap<>(), null);
    profilesMetadata.getProfiles().put("default", defaultProfile);
    var profilesLoader = new DummyProfilesLoader(profilesMetadata);
    var profileService = new ProfileService(profilesLoader, initialProfileConfigs, profileScope);

    profileService.initialize();

    profileService.addToWhitelist("default", "dsOmics");

    verify(profileScope).removeAllProfileBeans("default");
    assertTrue(profileService.getByName("default").getPackageWhitelist().contains("dsOmics"));
  }

  @Test
  void testUpdateLastImageId() {
    String profileName = "default";
    String oldImageId = "sha256:old";
    String newImageId = "sha256:new";

    // Create an existing profile config with oldImageId
    ProfileConfig existingProfile =
        ProfileConfig.create(
            profileName,
            "someImage",
            "localhost",
            6311,
            new HashSet<>(),
            new HashSet<>(),
            Map.of(),
            oldImageId);

    // Setup ProfilesMetadata and add existing profile
    ProfilesMetadata metadata = ProfilesMetadata.create();
    metadata.getProfiles().put(profileName, existingProfile);

    // Mock loader and dependencies
    ProfilesLoader loader = mock(ProfilesLoader.class);
    InitialProfileConfigs initialProfiles = mock(InitialProfileConfigs.class);
    ProfileScope profileScope = mock(ProfileScope.class);

    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProfileService profileService = new ProfileService(loader, initialProfiles, profileScope);
    profileService.initialize();

    // Act: update the image id
    profileService.updateLastImageId(profileName, newImageId);

    // Assert that the profile has been updated
    ProfileConfig updated = profileService.getByName(profileName);
    assertEquals(newImageId, updated.getLastImageId());

    // Verify flush and save were called
    verify(profileScope).removeAllProfileBeans(profileName);
    verify(loader).save(any());
  }
}
