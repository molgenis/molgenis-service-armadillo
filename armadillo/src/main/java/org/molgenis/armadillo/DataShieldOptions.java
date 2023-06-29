package org.molgenis.armadillo;

import com.google.common.collect.ImmutableMap;
import org.molgenis.r.RServerConnection;

public interface DataShieldOptions {
  ImmutableMap<String, String> getValue(RServerConnection connection);
}
