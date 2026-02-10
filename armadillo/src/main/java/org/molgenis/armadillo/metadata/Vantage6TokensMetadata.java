package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@AutoValue
public abstract class Vantage6TokensMetadata implements Persistable {

  @JsonIgnore
  public abstract ConcurrentMap<String, Vantage6Token> getTokens();

  @JsonProperty("tokens")
  public Map<String, Vantage6Token> getTokenMap() {
    return getTokens();
  }

  @JsonCreator
  public static Vantage6TokensMetadata create(
      @JsonProperty("tokens") Map<String, Vantage6Token> tokenMap) {
    ConcurrentMap<String, Vantage6Token> map = new ConcurrentHashMap<>();
    if (tokenMap != null) {
      map.putAll(tokenMap);
    }
    return new AutoValue_Vantage6TokensMetadata(map);
  }

  public static Vantage6TokensMetadata create() {
    return new AutoValue_Vantage6TokensMetadata(new ConcurrentHashMap<>());
  }
}
