package org.molgenis.armadillo.settings;

import com.google.auto.value.AutoValue;
import java.util.LinkedHashMap;
import java.util.Map;

@AutoValue
public abstract class ArmadilloSettings {
  public abstract Map<String, UserDetails> getUsers();

  public static ArmadilloSettings create() {
    return new AutoValue_ArmadilloSettings(new LinkedHashMap<>());
  }
}
