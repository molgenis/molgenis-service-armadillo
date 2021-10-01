package org.molgenis.armadillo;

import com.google.common.collect.ImmutableMap;

public interface DataShieldOptions {

  void init();

  ImmutableMap<String, String> getValue();
}
