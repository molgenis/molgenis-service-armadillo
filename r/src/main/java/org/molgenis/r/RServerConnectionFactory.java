package org.molgenis.r;

import java.io.IOException;
import java.net.*;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.rock.RockConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RServerConnectionFactory implements RConnectionFactory {
  private static final Logger logger = LoggerFactory.getLogger(RServerConnectionFactory.class);

  private final EnvironmentConfigProps environment;

  public RServerConnectionFactory(EnvironmentConfigProps environment) {
    this.environment = environment;
  }

  int doHead(String uri) {
    URL url;
    try {
      url = new URL(uri);
      HttpURLConnection connection;
      try {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.getContentLength();
        // -1 for rserve : `curl --http0.9 --head http://localhost:6311`
        // 200 for rock: `curl --head http://localhost:6311`
        return connection.getResponseCode();
      } catch (ConnectException e) {
        // server down
        return -99;
      } catch (SocketException e) {
        // Server not ready
        logger.info("Server not ready on " + url, e);
        return -2;
      } catch (IOException e) {
        logger.warn("Unexpected response on " + url, e);
      }
    } catch (MalformedURLException e) {
      logger.warn("Unexpected url", e);
    }
    return -3;
  }

  @Override
  public RServerConnection tryCreateConnection() {
    String url = "http://" + environment.getHost() + ":" + environment.getPort();
    int status = doHead(url);
    if (status == -99) {
      logger.warn("Container for '" + url + "'  is down");
    } else if (status == -2) {
      logger.warn("Container for '" + url + "' is not ready");
    } else if (status == 200) {
      logger.info("Container for '" + url + "' is running");
    }
    return new RockConnectionFactory(environment).tryCreateConnection();
  }
}
