package org.molgenis.armadillo;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.config.annotation.ProfileScope;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.Rserve.RConnection;
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

  private final ProfileConfigProps profileConfigProps;
  private final PackageService packageService;

  @SuppressWarnings("java:S3077") // ImmutableMap is thread-safe
  private volatile ImmutableMap<String, String> options;

  private final RConnectionFactory rConnectionFactory;

  public DataShieldOptionsImpl(
      ProfileConfigProps profileConfigProps,
      PackageService packageService,
      RConnectionFactory rConnectionFactory) {
    this.profileConfigProps = requireNonNull(profileConfigProps);
    this.packageService = requireNonNull(packageService);
    this.rConnectionFactory = requireNonNull(rConnectionFactory);
  }

  @PostConstruct
  public void init() {
    RConnection connection = null;
    var optionsBuilder = ImmutableMap.<String, String>builder();
    try {
      connection = rConnectionFactory.retryCreateConnection();
      packageService.getInstalledPackages(connection).stream()
          .map(RPackage::options)
          .filter(Objects::nonNull)
          .forEach(optionsBuilder::putAll);
      optionsBuilder.putAll(profileConfigProps.getOptions());
      options = optionsBuilder.build();
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
