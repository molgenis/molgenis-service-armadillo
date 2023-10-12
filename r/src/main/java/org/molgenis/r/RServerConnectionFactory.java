package org.molgenis.r;

import java.io.IOException;
import java.net.*;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.rock.RockConnectionFactory;
import org.molgenis.r.rserve.RserveConnectionFactory;
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
        // -1 for rserve
        // 200 for rock
        return connection.getResponseCode();
      } catch (ConnectException e) {
        // down
        return -99;
      } catch (SocketException e) {
        // Server not ready
        return -2;
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return -3;
  }

  @Override
  public RServerConnection tryCreateConnection() {
    logger.warn("================ IsRock testing ====================");
    String url = "http://" + environment.getHost() + ":" + environment.getPort();
    int status = doHead(url);
    if (status == -99) {
      // server down
      logger.warn("Container is down");
    } else if (status == -2) {
      logger.warn("Container service not ready");
    }
    boolean isRock = status == 200;
    logger.warn("================ " + isRock + " ====================");

    try {
      if (isRock) {
        if (environment.getName().contains("rock")) {
          logger.warn(
              "Using old name based rock setting. Please fix configuration for '"
                  + environment.getName()
                  + "'");
        }
        return new RockConnectionFactory(environment).tryCreateConnection();
      } else {
        return new RserveConnectionFactory(environment).tryCreateConnection();
      }
    } catch (Exception e) {
      logger.info("Not a Rock server [{}], trying Rserve...", e.getMessage(), e);
      return new RserveConnectionFactory(environment).tryCreateConnection();
    }
  }
}
