package org.molgenis.datashield;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MolgenisConfig {
  @Value("${molgenis.token}")
  private String token;

  @Value("${molgenis.uri}")
  private String molgenisRootUri;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplateBuilder()
        .messageConverters(
            List.of(new GsonHttpMessageConverter(),
                new AllEncompassingFormHttpMessageConverter(),
                new ResourceHttpMessageConverter(true)))
        .defaultHeader("X-Molgenis-Token", token)
        .rootUri(molgenisRootUri)
        .build();
  }
}
