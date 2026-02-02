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
            "default",
            "test",
            false,
            null,
            "localhost",
            1234,
            new HashSet<>(),
            Set.of(),
            new HashMap<>(),
            null,
            null,
            null,
            null,
            null,
            false);
    profilesMetadata.getProfiles().put("default", defaultProfile);
    var profilesLoader = new DummyProfilesLoader(profilesMetadata);
    var profileService = new ProfileService(profilesLoader, initialProfileConfigs, profileScope);

    profileService.initialize();

    profileService.addToWhitelist("default", "dsOmics");

    verify(profileScope).removeAllProfileBeans("default");
    assertTrue(profileService.getByName("default").getPackageWhitelist().contains("dsOmics"));
  }

  @Test
  void testUpdateMetaData() {
    String profileName = "default";
    String oldImageId = "sha256:old";
    String newImageId = "sha256:new";
    String newVersionId = "0.0.1";
    Long newImageSize = 123_456_789L;
    String newCreationDate = "2025-08-05T12:34:56Z";
    String newInstallDate = "2025-10-05T12:34:56Z";

    // Create an existing profile config with oldImageId
    ProfileConfig existingProfile =
        ProfileConfig.create(
            profileName,
            "someImage",
            false,
            null,
            "localhost",
            6311,
            new HashSet<>(),
            new HashSet<>(),
            Map.of(),
            oldImageId,
            null,
            null,
            null,
            null,
            false);

    // Setup ProfilesMetadata and add existing profile
    ProfilesMetadata metadata = ProfilesMetadata.create();
    metadata.getProfiles().put(profileName, existingProfile);

    // Mock loader and dependencies
    ProfilesLoader loader = mock(ProfilesLoader.class);
    InitialProfileConfigs initialProfiles = mock(InitialProfileConfigs.class);
    ProfileScope mockProfileScope = mock(ProfileScope.class);

    when(loader.load()).thenReturn(metadata);
    when(loader.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ProfileService profileService = new ProfileService(loader, initialProfiles, mockProfileScope);
    profileService.initialize();

    // Act: update the image id, version, and size
    profileService.updateImageMetaData(
        profileName,
        newImageId,
        newVersionId,
        newImageSize,
        newCreationDate,
        newInstallDate,
        false);

    // Assert that the profile has been updated
    ProfileConfig updated = profileService.getByName(profileName);
    assertEquals(newImageId, updated.getLastImageId());
    assertEquals(newVersionId, updated.getVersionId());
    assertEquals(newImageSize, updated.getImageSize());

    // Verify flush and save were called
    verify(mockProfileScope).removeAllProfileBeans(profileName);
    verify(loader).save(any());
  }
}
