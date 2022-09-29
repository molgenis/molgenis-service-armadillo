package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class ProfilesLoader extends StorageJsonLoader<ProfilesMetadata> {

  @Override
  public ProfilesMetadata createDefault() {
    return ProfilesMetadata.create();
  }

  @Override
  public Class<? extends Metadata> getTargetClass() {
    return ProfilesMetadata.class;
  }

  @Override
  public String getJsonFilename() {
    return "profiles.json";
  }
}
