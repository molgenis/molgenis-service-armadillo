package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.molgenis.armadillo.container.annotation.ProfileScope;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.springframework.stereotype.Component;

/**
 * Retrieves and combines armadillo options. These are defined by:
 *
 * <ol>
 *   <li>The Options fields in the * installed R packages
 *   <li>The rserve.options application properties
 * </ol>
 */
@Component
@ProfileScope
public class DataShieldOptionsImpl implements DataShieldOptions {

  private final ContainerConfig containerConfig;
  private final PackageService packageService;

  @SuppressWarnings("java:S3077") // ImmutableMap is thread-safe
  private volatile ImmutableMap<String, String> options;

  public DataShieldOptionsImpl(ContainerConfig containerConfig, PackageService packageService) {
    this.containerConfig = requireNonNull(containerConfig);
    this.packageService = requireNonNull(packageService);
  }

  private void init(RServerConnection connection) {
    if (options == null) {
      Map<String, String> optionsMap =
          packageService.getInstalledPackages(connection).stream()
              .map(RPackage::options)
              .filter(Objects::nonNull)
              .collect(HashMap::new, Map::putAll, Map::putAll);
      optionsMap.putAll(containerConfig.getOptions());
      options = ImmutableMap.copyOf(optionsMap);
    }
  }

  @Override
  public ImmutableMap<String, String> getValue(RServerConnection connection) {
    init(connection);
    return ImmutableMap.copyOf(options);
  }
}
