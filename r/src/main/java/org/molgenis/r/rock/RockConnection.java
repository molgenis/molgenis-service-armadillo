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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class RockConnection implements RServerConnection {

  private static final Logger logger = LoggerFactory.getLogger(RockConnection.class);
  public static final String FILE_DOWNLOAD_FAILED = "File download failed: ";
  public static final MediaType MEDIATYPE_APPLICATION_RSCRIPT =
      MediaType.valueOf("application/x-rscript");
  private static final String UPLOAD_FAILED = "File upload failed: ";
  private static final String UPLOAD_ENDPOINT = "/_upload";
  private static final String EVAL_ENDPOINT = "/_eval";
  public static final String DOWNLOAD_ENDPOINT = "/_download";
  private static final String PATH = "path";
  private static final String OVERWRITE = "overwrite";
  private static final String FILE = "file";

  private String rockSessionId;

  private final RockApplication application;
  private final RestClient restClient;

  public RockConnection(RockApplication application) throws RServerException {
    this.application = application;
    this.restClient = getRestClient();
    openSession();
  }

  @Override
  public RServerResult eval(String expr, boolean serialized) {
    String serverUrl = getRSessionResourceUrl(EVAL_ENDPOINT);
    if (serialized) {
      RestClient.RequestBodySpec request =
          createRequestForPost(serverUrl, expr, MEDIATYPE_APPLICATION_RSCRIPT);
      byte[] responseBody =
          request
              .headers(
                  httpHeaders -> {
                    httpHeaders.setAccept(
                        Lists.newArrayList(
                            MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
                  })
              .retrieve()
              .body(byte[].class);
      return new RockResult(responseBody);
    } else {
      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = createHeaders();
      headers.setContentType(MEDIATYPE_APPLICATION_RSCRIPT);
      headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
      ResponseEntity<String> response =
          restTemplate.exchange(
              serverUrl, HttpMethod.POST, new HttpEntity<>(expr, headers), String.class);
      String jsonSource = response.getBody();
      return new RockResult(jsonSource);
      //        RestClient.RequestBodySpec request = createRequestForPost(serverUrl, expr,
      // MEDIATYPE_APPLICATION_RSCRIPT);
      //        String responseBody = request.headers(httpHeaders -> {
      //        httpHeaders.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
      //      }).retrieve().body(String.class);
      //        return new RockResult(responseBody);
    }
  }

  @Override
  public void writeFile(String fileName, InputStream in) throws RServerException {
    try {
      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add(FILE, new MultiPartInputStreamResource(in, fileName));

      String serverUrl = getRSessionResourceUrl(UPLOAD_ENDPOINT);
      UriComponentsBuilder builder =
          UriComponentsBuilder.fromHttpUrl(serverUrl)
              .queryParam(PATH, fileName)
              .queryParam(OVERWRITE, true);

      ResponseEntity<Void> response =
          postToRestClient(builder.toUriString(), body, MediaType.MULTIPART_FORM_DATA)
              .toBodilessEntity();

      if (!response.getStatusCode().is2xxSuccessful()) {
        logger.error("File upload to {} failed: {}", serverUrl, response.getStatusCode());
        throw new RockServerException(UPLOAD_FAILED + response.getStatusCode());
      }
    } catch (RestClientException e) {
      throw new RockServerException(UPLOAD_FAILED, e);
    }
  }

  @Override
  public void readFile(String fileName, Consumer<InputStream> inputStreamConsumer)
      throws RServerException {
    try {
      HttpHeaders headers = createHeaders();
      headers.setAccept(Collections.singletonList(MediaType.ALL));

      String serverUrl = getRSessionResourceUrl(DOWNLOAD_ENDPOINT);

      UriComponentsBuilder builder =
          UriComponentsBuilder.fromHttpUrl(serverUrl).queryParam(PATH, fileName);
      //      RestClient.ResponseSpec response = restClient.get()
      //              .uri(builder.build().toUri())
      //              .headers(httpHeaders -> {
      //                headers.setAccept(Collections.singletonList(MediaType.ALL));
      //              }).retrieve();

      RestTemplate restTemplate = new RestTemplate();
      restTemplate.execute(
          builder.build().toUri(),
          HttpMethod.GET,
          request -> request.getHeaders().putAll(headers),
          (ResponseExtractor<Void>)
              resp -> {
                if (!resp.getStatusCode().is2xxSuccessful()) {
                  logger.error("File download from {} failed: {}", serverUrl, resp.getStatusCode());
                  throw new RuntimeException(FILE_DOWNLOAD_FAILED + resp.getStatusText());
                } else {
                  inputStreamConsumer.accept(resp.getBody());
                }
                return null;
              });
    } catch (RestClientException e) {
      throw new RockServerException(FILE_DOWNLOAD_FAILED, e);
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
      RockSessionInfo info =
          postToRestClient(
                  getRSessionsResourceUrl(),
                  new LinkedMultiValueMap<>(),
                  MediaType.APPLICATION_JSON)
              .body(RockSessionInfo.class);
      assert info != null;
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

  private String getAuthHeader() {
    String auth = application.getUser() + ":" + application.getPassword();
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
    return "Basic " + new String(encodedAuth);
  }

  private HttpHeaders createHeaders() {
    return new HttpHeaders() {
      {
        add("Authorization", getAuthHeader());
      }
    };
  }

  private RestClient getRestClient() {
    String serverUrl = getRSessionResourceUrl(UPLOAD_ENDPOINT);
    String authHeader = getAuthHeader();
    return RestClient.builder()
        .baseUrl(serverUrl)
        .defaultHeaders(
            httpHeaders -> {
              httpHeaders.setBasicAuth(authHeader);
              httpHeaders.set(HttpHeaders.AUTHORIZATION, authHeader);
            })
        .build();
  }

  private RestClient.RequestBodySpec createRequestForPost(
      String uriString, Object body, MediaType contentType) {
    return restClient.post().uri(uriString).contentType(contentType).body(body);
  }

  private RestClient.ResponseSpec postToRestClient(
      String uriString, Object body, MediaType contentType) {
    return createRequestForPost(uriString, body, contentType).retrieve();
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
