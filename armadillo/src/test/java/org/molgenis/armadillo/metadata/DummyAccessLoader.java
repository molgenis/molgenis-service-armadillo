package org.molgenis.armadillo.metadata;

public class DummyAccessLoader extends AccessLoader {

  private final AccessMetadata initialMetadata;

  public DummyAccessLoader() {
    this.initialMetadata = AccessMetadata.create();
  }

  public DummyAccessLoader(AccessMetadata initialMetadata) {
    this.initialMetadata = initialMetadata;
  }

  @Override
  public AccessMetadata save(AccessMetadata newMetadata) {
    return newMetadata;
  }

  @Override
  public AccessMetadata load() {
    return initialMetadata;
  }
}
