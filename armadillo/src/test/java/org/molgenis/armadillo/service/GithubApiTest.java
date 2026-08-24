package org.molgenis.armadillo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.gson.JsonElement;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GithubApiTest {

  @Mock HttpClient httpClient;
  @Mock HttpResponse<String> response;

  GithubApi githubApi;

  @BeforeEach
  void setUp() {
    githubApi = new GithubApi(httpClient);
  }

  @Test
  void getLastRelease_should_return_json_on_200() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"tag_name\":\"v1.0\"}");

    JsonElement result = githubApi.getLastRelease();

    assertEquals("v1.0", result.getAsJsonObject().get("tag_name").getAsString());
  }

  @Test
  void getLastRelease_should_throw_on_non_200() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(404);

    assertThrows(ResponseStatusException.class, () -> githubApi.getLastRelease());
  }

  @Test
  void getLastRelease_requestsCorrectUrl() throws Exception {
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.<String>send(requestCaptor.capture(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"tag_name\":\"v1.0\"}");

    githubApi.getLastRelease();

    assertEquals(
        "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/latest",
        requestCaptor.getValue().uri().toString());
  }

  @Test
  void getReleaseVersion_requestsCorrectUrl() throws Exception {
    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    when(httpClient.<String>send(requestCaptor.capture(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"tag_name\":\"v5.15.0\"}");

    JsonElement result = githubApi.getReleaseVersion("v5.15.0");

    assertEquals("v5.15.0", result.getAsJsonObject().get("tag_name").getAsString());
    assertEquals(
        "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/tags/v5.15.0",
        requestCaptor.getValue().uri().toString());
  }

  @Test
  void getReleaseTag_should_throw_on_non_200() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(response);
    when(response.statusCode()).thenReturn(500);

    assertThrows(ResponseStatusException.class, () -> githubApi.getReleaseVersion("v1.0.0"));
  }
}
