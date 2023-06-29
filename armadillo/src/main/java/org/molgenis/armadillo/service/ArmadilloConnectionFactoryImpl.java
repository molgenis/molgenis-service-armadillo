package org.molgenis.armadillo.service;

import static java.lang.String.format;

import java.util.Map.Entry;
import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.profile.annotation.ProfileScope;
import org.molgenis.r.Formatter;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.molgenis.r.service.PackageService;
import org.springframework.stereotype.Component;

@Component
@ProfileScope
public class ArmadilloConnectionFactoryImpl implements ArmadilloConnectionFactory {

  private final PackageService packageService;
  private final ProfileConfig profileConfig;
  private final DataShieldOptions dataShieldOptions;
  private final RConnectionFactory rConnectionFactory;

  public ArmadilloConnectionFactoryImpl(
      PackageService packageService,
      ProfileConfig profileConfig,
      DataShieldOptions dataShieldOptions,
      RConnectionFactory rConnectionFactory) {
    this.packageService = packageService;
    this.profileConfig = profileConfig;
    this.dataShieldOptions = dataShieldOptions;
    this.rConnectionFactory = rConnectionFactory;
  }

  @Override
  public RServerConnection createConnection() {
    try {
      RServerConnection connection = rConnectionFactory.tryCreateConnection();
      loadPackages(connection);
      setDataShieldOptions(connection);
      return connection;
    } catch (RServerException cause) {
      throw new ConnectionCreationFailedException(cause);
    }
  }

  private void loadPackages(RServerConnection connection) {
    packageService.loadPackages(connection, profileConfig.getPackageWhitelist());
  }

  private void setDataShieldOptions(RServerConnection connection) throws RServerException {
    for (Entry<String, String> option : dataShieldOptions.getValue(connection).entrySet()) {
      connection.eval(
          format(
              "base::options(%s = %s)",
              option.getKey(), Formatter.quoteIfAlphaNumeric(option.getValue())));
    }
  }
}
