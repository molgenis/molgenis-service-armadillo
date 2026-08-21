package org.molgenis.armadillo.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GithubApi {

  private static final String RELEASE_URL =
      "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/latest";
  private static final String TAG_URL =
      "https://api.github.com/repos/molgenis/molgenis-service-armadillo/releases/tags/";

  private final HttpClient httpClient;

  public GithubApi(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public JsonElement getReleaseTag(String tag) throws IOException, InterruptedException {
    return getReleaseFromGithub(TAG_URL + tag);
  }

  public JsonElement getLastRelease() throws IOException, InterruptedException {
    return getReleaseFromGithub(RELEASE_URL);
  }

  private JsonElement getReleaseFromGithub(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      return JsonParser.parseString(response.body()).getAsJsonObject();
    } else {
      throw new ResponseStatusException(HttpStatusCode.valueOf(response.statusCode()));
    }
  }
}
