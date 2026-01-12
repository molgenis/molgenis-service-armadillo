package org.molgenis.armadillo.container;

import java.awt.*;

public interface ContainerWhitelister {

  /*
   * Executes the specific logic to add a package to the container's whitelist.
   *
   * @param config The generic ContainerConfig object to be modified
   * @param pack The package name to add to the whitelist.
   */

  boolean supports(ContainerConfig config);

  void addToWhitelist(ContainerConfig config, String pack);
}
