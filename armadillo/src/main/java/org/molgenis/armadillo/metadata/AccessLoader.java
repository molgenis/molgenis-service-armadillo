package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class AccessLoader extends StorageJsonLoader<AccessMetadata> {

  @Override
  public AccessMetadata createDefault() {
    return AccessMetadata.create();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return AccessMetadata.class;
  }

  @Override
  public String getJsonFilename() {
    return "access.json";
  }
}
