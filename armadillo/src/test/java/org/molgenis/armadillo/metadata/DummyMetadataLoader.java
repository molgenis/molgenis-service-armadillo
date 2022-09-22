package org.molgenis.armadillo.metadata;

public class DummyMetadataLoader implements MetadataLoader {

  @Override
  public ArmadilloMetadata save(ArmadilloMetadata newMetadata) {
    return newMetadata;
  }

  @Override
  public ArmadilloMetadata load() {
    return ArmadilloMetadata.create();
  }
}
