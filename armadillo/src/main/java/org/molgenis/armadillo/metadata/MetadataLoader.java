package org.molgenis.armadillo.metadata;

public interface MetadataLoader {

  ArmadilloMetadata save(ArmadilloMetadata metadata);

  ArmadilloMetadata load();
}
