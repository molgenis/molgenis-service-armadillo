package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class AuthLoader extends StorageJsonLoader<OidcDetails> {

  @Override
  public OidcDetails createDefault() {
    return OidcDetails.create();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return OidcDetails.class;
  }

  @Override
  public String getJsonFilename() {
    return "auth.json";
  }
}
