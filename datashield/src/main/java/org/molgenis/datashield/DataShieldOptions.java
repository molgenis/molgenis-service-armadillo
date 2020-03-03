package org.molgenis.datashield;

import com.google.common.collect.ImmutableMap;

public interface DataShieldOptions {
  ImmutableMap<String, String> getValue();
}
