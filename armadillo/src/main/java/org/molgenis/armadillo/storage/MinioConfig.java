package org.molgenis.armadillo.storage;

import static org.molgenis.armadillo.storage.MinioStorageService.MINIO_URL_PROPERTY;

import io.minio.MinioClient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@ConditionalOnProperty(MINIO_URL_PROPERTY)
@Configuration
@ConfigurationProperties("minio")
@Validated
public class MinioConfig {
  @NotBlank private String accessKey;
  @NotBlank private String secretKey;
  @NotBlank private String url = "http://localhost";
  @Positive private int port = 9000;
  private String region = null;

  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
        .credentials(accessKey, secretKey)
        .region(region)
        .endpoint(url + ":" + port)
        .build();
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
