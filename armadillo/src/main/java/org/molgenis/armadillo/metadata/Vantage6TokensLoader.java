package org.molgenis.armadillo.metadata;

import org.springframework.stereotype.Service;

@Service
public class Vantage6TokensLoader extends StorageJsonLoader<Vantage6TokensMetadata> {

  @Override
  public Vantage6TokensMetadata createDefault() {
    return Vantage6TokensMetadata.create();
  }

  @Override
  public Class<? extends Persistable> getTargetClass() {
    return Vantage6TokensMetadata.class;
  }

  @Override
  public String getJsonFilename() {
    return "v6-tokens.json";
  }
}
