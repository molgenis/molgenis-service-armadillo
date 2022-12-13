package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import javax.ws.rs.ProcessingException;
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
    var dockerClient =
        DockerClientBuilder.getInstance(
                DefaultDockerClientConfig.createDefaultConfigBuilder().build())
            .build();
    try {
      dockerClient.infoCmd().exec();
    } catch (ProcessingException e) {
      LOG.warn(
          "Docker management is enabled but Armadillo could not connect to it. Either "
              + "Docker is offline or it is not configure correctly.");
    }
    return dockerClient;
  }
}
