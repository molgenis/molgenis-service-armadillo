package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class AuthLoader extends StorageJsonLoader<AuthMetadata> {

  @Override
  public AuthMetadata createDefault() {
    return AuthMetadata.create();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return AuthMetadata.class;
  }

  @Override
  public String getJsonFilename() {
    return "auth.json";
  }
}
