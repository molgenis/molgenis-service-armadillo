package org.molgenis.datashield.r;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component
@SessionScope
public class RDatashieldSession {
  private RSession rSession = null;

  @Autowired
  private RConnectionFactory rConnectionFactory;

  public <T> T execute(RConnectionConsumer<T> consumer) throws RserveException, REXPMismatchException
  {
    RConnection connection = getRConnection();
    try {
      return consumer.accept(connection);
    }
    finally {
      rSession = connection.detach();
    }
  }

  private RConnection getRConnection() {
    try {
      if (rSession == null) {
        return rConnectionFactory.getNewConnection(false);
      }
      return rSession.attach();
    } catch (RserveException err) {
      throw new RuntimeException("foutje", err);
    }
  }
}
