package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import jakarta.ws.rs.ProcessingException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class DockerClientConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DockerClientConfig.class);

  @Bean
  DockerClient getDockerClient() {
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
      LOG.warn(
          "Docker management is enabled but Armadillo could not connect to it. Either "
              + "Docker is offline or it is not configured correctly.");
    }
    return dockerClient;
  }
}
