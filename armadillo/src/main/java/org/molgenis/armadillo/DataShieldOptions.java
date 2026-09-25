package org.molgenis.armadillo;

import com.google.common.collect.ImmutableMap;
import org.molgenis.connection.datashield.RServerConnection;

public interface DataShieldOptions {
  ImmutableMap<String, String> getValue(RServerConnection connection);
}
