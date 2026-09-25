package org.molgenis.armadillo;

import com.google.common.collect.ImmutableMap;
import org.molgenis.connection.RServerConnection;

public interface DataShieldOptions {
  ImmutableMap<String, String> getValue(RServerConnection connection);
}
