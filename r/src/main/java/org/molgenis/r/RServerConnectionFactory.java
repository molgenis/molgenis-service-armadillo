package org.molgenis.r;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
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

  String getMessageFromStatus(RockStatusCode statusCode, String url) {
    if (statusCode == RockStatusCode.SERVER_DOWN) {
      return ("Container for '" + url + "'  is down");
    } else if (statusCode == RockStatusCode.SERVER_NOT_READY) {
      return ("Container for '" + url + "' is not ready");
    } else if (statusCode == RockStatusCode.OK) {
      return ("Container for '" + url + "' is running");
    } else if (statusCode == RockStatusCode.UNEXPECTED_RESPONSE_CODE) {
      return ("Unexpected response code on " + url);
    } else if (statusCode == RockStatusCode.UNEXPECTED_RESPONSE) {
      return ("Unexpected response on " + url);
    } else if (statusCode == RockStatusCode.UNEXPECTED_URL) {
      return ("MalformedURLException on " + url);
    } else {
      return "";
    }
  }

  @Override
  public RServerConnection tryCreateConnection() {
    try {
      if (environment.getImage().contains("rock")) {
        String url = "http://" + environment.getHost() + ":" + environment.getPort();
        RockStatusCode rockStatus = doHead(url);
        String statusMessage = getMessageFromStatus(rockStatus, url);
        if (rockStatus == RockStatusCode.SERVER_DOWN
            || rockStatus == RockStatusCode.SERVER_NOT_READY
            || rockStatus == RockStatusCode.UNEXPECTED_URL) {
          logger.warn(statusMessage);
        } else if (!Objects.equals(statusMessage, "")) {
          logger.info(statusMessage);
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
