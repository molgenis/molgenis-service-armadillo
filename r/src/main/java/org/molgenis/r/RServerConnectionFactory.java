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

  RockStatusCode doHead(String uri) {
    URL url;
    try {
      url = new URL(uri);
      HttpURLConnection connection;
      try {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.getContentLength();
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
          return RockStatusCode.OK;
        }
        logger.info("Unexpected response code " + responseCode + " on " + url);
        return RockStatusCode.UNEXPECTED_RESPONSE_CODE;
      } catch (ConnectException e) {
        return RockStatusCode.SERVER_DOWN;
      } catch (SocketException e) {
        logger.info("Server not ready on " + url, e);
        return RockStatusCode.SERVER_NOT_READY;
      } catch (IOException e) {
        logger.warn("IOException response on " + url, e);
        return RockStatusCode.UNEXPECTED_RESPONSE;
      }
    } catch (MalformedURLException e) {
      logger.warn("MalformedURLException url", e);
      return RockStatusCode.UNEXPECTED_URL;
    }
  }

  @Override
  public RServerConnection tryCreateConnection() {
    try {
      if (environment.getImage().contains("rock")) {
        String url = "http://" + environment.getHost() + ":" + environment.getPort();
        RockStatusCode rockStatus = doHead(url);
        if (rockStatus == RockStatusCode.SERVER_DOWN) {
          logger.warn("Container for '" + url + "'  is down");
        } else if (rockStatus == RockStatusCode.SERVER_NOT_READY) {
          logger.warn("Container for '" + url + "' is not ready");
        } else if (rockStatus == RockStatusCode.OK) {
          logger.info("Container for '" + url + "' is running");
        } else if (rockStatus == RockStatusCode.UNEXPECTED_RESPONSE_CODE) {
          logger.info("Unexpected response code on " + url);
        } else if (rockStatus == RockStatusCode.UNEXPECTED_RESPONSE) {
          logger.info("Unexpected response on " + url);
        } else if (rockStatus == RockStatusCode.UNEXPECTED_URL) {
          logger.warn("MalformedURLException on " + url);
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

enum RockStatusCode {
  SERVER_DOWN(-99),
  SERVER_NOT_READY(-2),
  UNEXPECTED_URL(-3),
  UNEXPECTED_RESPONSE(-1),
  UNEXPECTED_RESPONSE_CODE(-4),
  OK(200);

  private final int code;

  RockStatusCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
