package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * Retrieves and combines armadillo options. These are defined by:
 *
 * <ol>
 *   <li>The Options fields in the * installed R packages
 *   <li>The rserve.options application properties
 * </ol>
 */
public class DataShieldOptionsImpl implements DataShieldOptions {

  private final ProfileConfigProps profileConfigProps;
  private final PackageService packageService;
  private Map<String, String> options;
  private final RConnectionFactory rConnectionFactory;

  public DataShieldOptionsImpl(
      ProfileConfigProps profileConfigProps,
      PackageService packageService,
      RConnectionFactory rConnectionFactory) {
    this.profileConfigProps = requireNonNull(profileConfigProps);
    this.packageService = requireNonNull(packageService);
    this.rConnectionFactory = requireNonNull(rConnectionFactory);
  }

  @Override
  public void init() {
    RConnection connection = null;
    try {
      connection = rConnectionFactory.retryCreateConnection();
      options =
          packageService.getInstalledPackages(connection).stream()
              .map(RPackage::options)
              .filter(Objects::nonNull)
              .collect(HashMap::new, Map::putAll, Map::putAll);
      options.putAll(profileConfigProps.getOptions());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Override
  public ImmutableMap<String, String> getValue() {
    return ImmutableMap.copyOf(options);
  }
}
