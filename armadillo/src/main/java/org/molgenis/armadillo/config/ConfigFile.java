package org.molgenis.armadillo.config;

import java.util.Map;
import org.molgenis.armadillo.metadata.OidcDetails;

public interface ConfigFile {

  void update(OidcDetails oidcDetails);

  Map<String, Object> getConfig();
}
