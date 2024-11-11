package org.molgenis.r;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.molgenis.r.config.EnvironmentConfigProps;

public class RserverConnectionFactoryTest {
  EnvironmentConfigProps props = new EnvironmentConfigProps();
  RServerConnectionFactory connectionFactory = new RServerConnectionFactory(props);
  String url = "http://my-url.nl";

  @Test
  public void testGetMessageFromStatusDown() {
    String actual = connectionFactory.getMessageFromStatus(RockStatusCode.SERVER_DOWN, url);
    String expected = "Container for '" + url + "'  is down";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetMessageFromStatusServerNotReady() {
    String actual = connectionFactory.getMessageFromStatus(RockStatusCode.SERVER_NOT_READY, url);
    String expected = "Container for '" + url + "' is not ready";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetMessageFromStatusOk() {
    String actual = connectionFactory.getMessageFromStatus(RockStatusCode.OK, url);
    String expected = "Container for '" + url + "' is running";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetMessageFromStatusUnexpectedResponseCode() {
    String actual =
        connectionFactory.getMessageFromStatus(RockStatusCode.UNEXPECTED_RESPONSE_CODE, url);
    String expected = "Unexpected response code on " + url;
    assertEquals(expected, actual);
  }

  @Test
  public void testGetMessageFromStatusUnexpectedResponse() {
    String actual = connectionFactory.getMessageFromStatus(RockStatusCode.UNEXPECTED_RESPONSE, url);
    String expected = "Unexpected response on " + url;
    assertEquals(expected, actual);
  }

  @Test
  public void testGetMessageFromStatusUnexpectedUrl() {
    String actual = connectionFactory.getMessageFromStatus(RockStatusCode.UNEXPECTED_URL, url);
    String expected = "MalformedURLException on " + url;
    assertEquals(expected, actual);
  }
}
