package org.molgenis.armadillo.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServeEnvironments;
import org.molgenis.r.service.PackageService;

@ExtendWith(MockitoExtension.class)
class ProfilesTest {

  @Mock private RServeEnvironments rServeEnvironments;
  @Mock private PackageService packageService;
  @Mock private RConnectionFactory environment1Factory;
  @Mock private RConnectionFactory environment2Factory;
  private DataShieldConfigProps dataShieldConfigProps;

  private Profiles profiles;

  @BeforeEach
  void setUp() {
    ProfileConfigProps profile1 = new ProfileConfigProps();
    profile1.setEnvironment("environment1");
    profile1.setName("default");
    profile1.setOptions(Map.of("foo", "bar"));
    ProfileConfigProps profile2 = new ProfileConfigProps();
    profile2.setEnvironment("environment2");
    profile2.setName("exposome");
    profile2.setOptions(Map.of("foo", "foobar"));
    List<ProfileConfigProps> profileConfigProps = List.of(profile1, profile2);
    when(rServeEnvironments.getConnectionFactory("environment1")).thenReturn(environment1Factory);
    when(rServeEnvironments.getConnectionFactory("environment2")).thenReturn(environment2Factory);
    dataShieldConfigProps = new DataShieldConfigProps(profileConfigProps);
    profiles = new Profiles(rServeEnvironments, packageService, dataShieldConfigProps);
    profiles.init();
  }

  @Test
  void getProfiles() {
    var children = profiles.getProfiles();
    assertEquals(2, children.size());
    assertEquals(
        Set.of("default", "exposome"),
        children.stream().map(Profile::getProfileName).collect(Collectors.toSet()));
  }

  @Test
  void getDefaultProfile() {
    assertEquals("default", profiles.getDefaultProfile().getProfileName());
  }

  @Test
  void getProfileByName() {
    assertEquals("exposome", profiles.getProfileByName("exposome").get().getProfileName());
  }
}
