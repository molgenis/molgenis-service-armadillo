package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class ContainersLoader extends StorageJsonLoader<ContainersMetadata> {

  @Override
  public ContainersMetadata createDefault() {
    return ContainersMetadata.create();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return ContainersMetadata.class;
  }

  @Override
  public ContainersMetadata load() {
    return super.load();
  }

  @Override
  public String getJsonFilename() {
    return "containers.json";
  }
}
