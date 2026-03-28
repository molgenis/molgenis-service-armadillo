package org.molgenis.armadillo.metadata;

public class DummyContainersLoader extends ContainersLoader {

  private final ContainersMetadata initialMetadata;

  public DummyContainersLoader() {
    this.initialMetadata = ContainersMetadata.create();
  }

  public DummyContainersLoader(ContainersMetadata initialMetadata) {
    this.initialMetadata = initialMetadata;
  }

  @Override
  public ContainersMetadata save(ContainersMetadata newMetadata) {
    return newMetadata;
  }

  @Override
  public ContainersMetadata load() {
    return initialMetadata;
  }
}
