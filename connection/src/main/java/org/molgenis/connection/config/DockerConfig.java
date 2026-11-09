package org.molgenis.connection.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import jakarta.ws.rs.ProcessingException;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;

@Configuration
public class DockerConfig {

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
    } catch (ProcessingException e) {
      throw new DisabledException(
          "Docker is offline or it is not configured correctly. Docker management and flower data upload are disabled.");
    }
    return dockerClient;
  }
}
