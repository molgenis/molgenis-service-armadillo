package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.controller.ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(DOCKER_MANAGEMENT_ENABLED)
public class DockerClientConfig {
  @Bean
  DockerClient getDockerClient() {
    return DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder())
        .build();
  }
}
