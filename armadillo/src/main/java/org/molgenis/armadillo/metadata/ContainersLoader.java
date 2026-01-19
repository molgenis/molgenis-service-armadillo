package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class ContainersLoader extends StorageJsonLoader<ContainersMetadata> {

  @Override
  public ContainersMetadata createDefault() {
    return ContainersMetadata.createEmpty();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return ContainersMetadata.class;
  }

  @Override
  public ContainersMetadata load() {
    try {
      System.out.println("!!! DEBUG: About to call super.load()...");
      ContainersMetadata result = super.load();

      // If we get here, the parent "succeeded" but maybe returned an empty object
      System.out.println("!!! DEBUG: super.load() returned object: " + result);
      return result;

    } catch (Exception e) {
      // This will only catch if super.load() throws an exception instead of catching it internally
      System.out.println("!!! DEBUG: super.load() THREW EXCEPTION: " + e.getClass().getName());
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public String getJsonFilename() {
    String filename = "containers.json";
    System.out.println("!!! DEBUG: Parent is asking for filename. Giving: " + filename);
    return filename;
  }
}
