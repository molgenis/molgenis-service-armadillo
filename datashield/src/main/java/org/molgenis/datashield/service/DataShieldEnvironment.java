package org.molgenis.datashield.service;

import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.DefaultDSEnvironment;

public class DataShieldEnvironment extends DefaultDSEnvironment {

  public DataShieldEnvironment(DSMethodType dsMethodType) {
    super(dsMethodType);
  }
}
