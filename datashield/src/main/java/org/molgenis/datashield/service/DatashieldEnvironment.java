package org.molgenis.datashield.service;

import org.obiba.datashield.core.DSMethodType;
import org.obiba.datashield.core.impl.DefaultDSEnvironment;

public class DatashieldEnvironment extends DefaultDSEnvironment {

  public DatashieldEnvironment(DSMethodType dsMethodType) {
    super(dsMethodType);
  }
}
