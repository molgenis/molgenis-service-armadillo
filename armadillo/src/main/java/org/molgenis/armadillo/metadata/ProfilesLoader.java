package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class ProfilesLoader extends StorageJsonLoader<ProfilesMetadata> {

  @Override
  public ProfilesMetadata createDefault() {
    return ProfilesMetadata.create();
  }

  @Override
  public String getJsonFilename() {
    return "profiles.json";
  }
}
