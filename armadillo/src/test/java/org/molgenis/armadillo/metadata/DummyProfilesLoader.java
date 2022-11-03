package org.molgenis.armadillo.metadata;

public class DummyProfilesLoader extends ProfilesLoader {

  private final ProfilesMetadata initialMetadata;

  public DummyProfilesLoader() {
    this.initialMetadata = ProfilesMetadata.create();
  }

  public DummyProfilesLoader(ProfilesMetadata initialMetadata) {
    this.initialMetadata = initialMetadata;
  }

  @Override
  public ProfilesMetadata save(ProfilesMetadata newMetadata) {
    return newMetadata;
  }

  @Override
  public ProfilesMetadata load() {
    return initialMetadata;
  }
}
