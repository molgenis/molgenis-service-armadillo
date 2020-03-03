package org.molgenis.datashield;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@ConfigurationProperties(prefix = "molgenis")
@Configuration
public class MolgenisConfig {
  private String token;
  private String uri;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplateBuilder()
        .messageConverters(
            List.of(
                new GsonHttpMessageConverter(),
                new AllEncompassingFormHttpMessageConverter(),
                new ResourceHttpMessageConverter(true)))
        .defaultHeader("X-Molgenis-Token", token)
        .rootUri(uri)
        .build();
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
