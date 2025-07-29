package org.molgenis.armadillo.profile;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class dockerApi {
  public static void main(String[] args) {
    String url = "http://localhost:2375/containers/json"; // URL for Docker API to list containers

    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(url);
      HttpResponse response = client.execute(request);

      // Print the response
      String responseString = EntityUtils.toString(response.getEntity());
      System.out.println(responseString);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
