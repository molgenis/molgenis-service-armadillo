package org.molgenis.armadillo.metadata;

public class DummyMetadataLoader extends MetadataLoader {

  private final ArmadilloMetadata initialMetadata;

  public DummyMetadataLoader() {
    this.initialMetadata = ArmadilloMetadata.create();
  }

  public DummyMetadataLoader(ArmadilloMetadata initialMetadata) {
    this.initialMetadata = initialMetadata;
  }

  @Override
  public ArmadilloMetadata save(ArmadilloMetadata newMetadata) {
    return newMetadata;
  }

  @Override
  public ArmadilloMetadata load() {
    return initialMetadata;
  }
}
