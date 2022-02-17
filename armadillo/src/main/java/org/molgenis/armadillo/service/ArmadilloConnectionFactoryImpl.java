package org.molgenis.armadillo.service;

import static java.lang.String.format;

import java.util.Map.Entry;
import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.config.annotation.ProfileScope;
import org.molgenis.r.Formatter;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
@ProfileScope
public class ArmadilloConnectionFactoryImpl implements ArmadilloConnectionFactory {

  private final DataShieldOptions dataShieldOptions;
  private final RConnectionFactory rConnectionFactory;

  public ArmadilloConnectionFactoryImpl(
      DataShieldOptions dataShieldOptions, RConnectionFactory rConnectionFactory) {
    this.dataShieldOptions = dataShieldOptions;
    this.rConnectionFactory = rConnectionFactory;
  }

  @Override
  public RConnection createConnection() {
    try {
      RConnection connection = rConnectionFactory.tryCreateConnection();
      setDataShieldOptions(connection);
      return connection;
    } catch (RserveException cause) {
      throw new ConnectionCreationFailedException(cause);
    }
  }

  private void setDataShieldOptions(RConnection con) throws RserveException {
    for (Entry<String, String> option : dataShieldOptions.getValue().entrySet()) {
      con.eval(
          format(
              "base::options(%s = %s)",
              option.getKey(), Formatter.quoteIfAlphaNumeric(option.getValue())));
    }
  }
}
