package org.molgenis.armadillo.service;

import static java.lang.String.format;
import static org.molgenis.armadillo.ArmadilloUtils.TABLE_ENV;

import java.util.Map.Entry;
import org.molgenis.armadillo.ArmadilloOptions;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
public class ArmadilloConnectionFactoryImpl implements ArmadilloConnectionFactory {

  private final ArmadilloOptions armadilloOptions;
  private final RConnectionFactory rConnectionFactory;

  public ArmadilloConnectionFactoryImpl(
      ArmadilloOptions armadilloOptions, RConnectionFactory rConnectionFactory) {
    this.armadilloOptions = armadilloOptions;
    this.rConnectionFactory = rConnectionFactory;
  }

  @Override
  public RConnection createConnection() {
    try {
      RConnection connection = rConnectionFactory.createConnection();
      createTableEnvironment(connection);
      setArmadilloOptions(connection);
      return connection;
    } catch (RserveException cause) {
      throw new ConnectionCreationFailedException(cause);
    }
  }

  private void setArmadilloOptions(RConnection con) throws RserveException {
    for (Entry<String, String> option : armadilloOptions.getValue().entrySet()) {
      con.eval(format("base::options(%s = %s)", option.getKey(), option.getValue()));
    }
  }

  private void createTableEnvironment(RConnection connection) throws RserveException {
    connection.eval(format("%s <- base::new.env()", TABLE_ENV));
  }
}
