package org.molgenis.datashield;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;

@Configuration
@ConfigurationProperties("minio.user")
public class MinioUserStorageConfig {
  private String accessKey;
  private String secretKey;
  private boolean secure = false;
  private String bucket = "datashield";
  private String url = "http://localhost";
  private int port = 9000;
  private String region = null;

  @Bean
  public IdGenerator idGenerator() {
    return new JdkIdGenerator();
  }

  @Bean("userStorageClient")
  public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
    return new MinioClient(url, port, accessKey, secretKey, region, secure);
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

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
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
