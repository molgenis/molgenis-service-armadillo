package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.container.annotation.ContainerScope;
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
@ContainerScope
public class DataShieldOptionsImpl implements DataShieldOptions {

  private final DatashieldContainerConfig datashieldContainerConfig;
  private final PackageService packageService;

  @SuppressWarnings("java:S3077") // ImmutableMap is thread-safe
  private volatile ImmutableMap<String, String> options;

  public DataShieldOptionsImpl(ContainerConfig config, PackageService packageService) {
    if (!(config instanceof DatashieldContainerConfig dsConfig)) {
      throw new IllegalArgumentException(
          "DataShieldOptionsImpl requires a DatashieldContainerConfig.");
    }
    this.datashieldContainerConfig = dsConfig;
    this.packageService = requireNonNull(packageService);
  }

  private void init(RServerConnection connection) {
    if (options == null) {
      Map<String, String> optionsMap =
          packageService.getInstalledPackages(connection).stream()
              .map(RPackage::options)
              .filter(Objects::nonNull)
              .collect(HashMap::new, Map::putAll, Map::putAll);
      optionsMap.putAll(datashieldContainerConfig.getOptions());
      options = ImmutableMap.copyOf(optionsMap);
    }
  }

  @Override
  public ImmutableMap<String, String> getValue(RServerConnection connection) {
    init(connection);
    return ImmutableMap.copyOf(options);
  }
}
