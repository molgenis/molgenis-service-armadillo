package org.molgenis.connection.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerConfig {
  private static final Logger logger = LoggerFactory.getLogger(DockerConfig.class);

  @Bean
  public DockerClient dockerClient() {
    DefaultDockerClientConfig config =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient =
        new ZerodepDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();
    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

    try {
      dockerClient.infoCmd().exec();
    } catch (RuntimeException e) {
      logger.warn(
          "Docker is offline or not configured correctly. Docker management and flower data upload are disabled.",
          e);
    }
    return dockerClient;
  }
}
