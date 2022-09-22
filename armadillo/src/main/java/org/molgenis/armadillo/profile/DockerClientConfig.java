package org.molgenis.armadillo.profile;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerClientConfig {
  @Bean
  DockerClient getDockerClient() {
    return DockerClientBuilder.getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder())
        .build();
  }
}
