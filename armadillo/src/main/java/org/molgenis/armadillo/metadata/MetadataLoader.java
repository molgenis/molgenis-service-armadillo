package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class MetadataLoader extends StorageJsonLoader<ArmadilloMetadata> {

  @Override
  public ArmadilloMetadata createDefault() {
    return ArmadilloMetadata.create();
  }

  @Override
  public String getJsonFilename() {
    return "metadata.json";
  }
}
