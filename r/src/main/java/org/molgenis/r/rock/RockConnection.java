package org.molgenis.r.rock;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.function.Consumer;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class RockConnection implements RServerConnection {

  private static final Logger logger = LoggerFactory.getLogger(RockConnection.class);

  private String rockSessionId;

  private final RockApplication application;

  public RockConnection(RockApplication application) throws RServerException {
    this.application = application;
    openSession();
  }

  @Override
  public RServerResult eval(String expr, boolean serialized) throws RServerException {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = createHeaders();
    headers.setContentType(MediaType.valueOf("application/x-rscript"));

    String serverUrl = getRSessionResourceUrl("/_eval");
    if (serialized) {
      // accept application/octet-stream
      headers.setAccept(
          Lists.newArrayList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
      ResponseEntity<byte[]> response =
          restTemplate.exchange(
              serverUrl, HttpMethod.POST, new HttpEntity<>(expr, headers), byte[].class);
      return new RockResult(response.getBody());
    } else {
      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
      ResponseEntity<String> response =
          restTemplate.exchange(
              serverUrl, HttpMethod.POST, new HttpEntity<>(expr, headers), String.class);
      String jsonSource = response.getBody();
      return new RockResult(jsonSource);
    }
  }

  @Override
  public void writeFile(String fileName, InputStream in) throws RServerException {
    try {
      HttpHeaders headers = createHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new MultiPartInputStreamResource(in, fileName));
      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

      String serverUrl = getRSessionResourceUrl("/_upload");
      UriComponentsBuilder builder =
          UriComponentsBuilder.fromHttpUrl(serverUrl)
              .queryParam("path", fileName)
              .queryParam("overwrite", true);

      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<String> response =
          restTemplate.postForEntity(builder.toUriString(), requestEntity, String.class);
      if (!response.getStatusCode().is2xxSuccessful()) {
        logger.error("File upload to {} failed: {}", serverUrl, response.getStatusCode());
        throw new RockServerException("File upload failed: " + response.getStatusCode());
      }
    } catch (RestClientException e) {
      throw new RockServerException("File upload failed", e);
    }
  }

  @Override
  public void readFile(String fileName, Consumer<InputStream> inputStreamConsumer)
      throws RServerException {
    try {
      HttpHeaders headers = createHeaders();
      headers.setAccept(Collections.singletonList(MediaType.ALL));

      String serverUrl = getRSessionResourceUrl("/_download");
      UriComponentsBuilder builder =
          UriComponentsBuilder.fromHttpUrl(serverUrl).queryParam("path", fileName);

      RestTemplate restTemplate = new RestTemplate();
      restTemplate.execute(
          builder.build().toUri(),
          HttpMethod.GET,
          request -> request.getHeaders().putAll(headers),
          (ResponseExtractor<Void>)
              response -> {
                if (!response.getStatusCode().is2xxSuccessful()) {
                  logger.error(
                      "File download from {} failed: {}", serverUrl, response.getStatusCode());
                  throw new RuntimeException("File download failed: " + response.getStatusText());
                } else {
                  inputStreamConsumer.accept(response.getBody());
                }
                return null;
              });
    } catch (RestClientException e) {
      throw new RockServerException("File download failed", e);
    }
  }

  @Override
  public boolean close() {
    if (Strings.isNullOrEmpty(rockSessionId)) return true;

    try {
      RestTemplate restTemplate = new RestTemplate();
      restTemplate.exchange(
          getRSessionResourceUrl(""),
          HttpMethod.DELETE,
          new HttpEntity<>(createHeaders()),
          Void.class);
      this.rockSessionId = null;
      return true;
    } catch (RestClientException e) {
      String msg = "Failure when closing the Rock R session {}";
      if (logger.isDebugEnabled()) logger.warn(msg, rockSessionId, e);
      else logger.warn(msg, rockSessionId);
    }
    return false;
  }

  private void openSession() throws RServerException {
    try {
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<RockSessionInfo> response =
          restTemplate.exchange(
              getRSessionsResourceUrl(),
              HttpMethod.POST,
              new HttpEntity<>(createHeaders()),
              RockSessionInfo.class);
      RockSessionInfo info = response.getBody();
      this.rockSessionId = info.getId();
    } catch (RestClientException e) {
      throw new RockServerException("Failure when opening a Rock R session", e);
    }
  }

  private String getRSessionsResourceUrl() {
    return String.format("%s/r/sessions", application.getUrl());
  }

  private String getRSessionResourceUrl(String path) {
    return String.format("%s/r/session/%s%s", application.getUrl(), rockSessionId, path);
  }

  private HttpHeaders createHeaders() {
    return new HttpHeaders() {
      {
        String auth = application.getUser() + ":" + application.getPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        add("Authorization", authHeader);
      }
    };
  }

  private static class MultiPartInputStreamResource extends InputStreamResource {

    private final String fileName;

    public MultiPartInputStreamResource(InputStream inputStream, String fileName) {
      super(inputStream, fileName);
      this.fileName = fileName;
    }

    @Override
    public String getFilename() {
      return fileName;
    }

    @Override
    public long contentLength() {
      return -1; // we do not want to generally read the whole stream into memory ...
    }
  }
}
