package org.molgenis.armadillo.info;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RMetrics {

  @Bean
  MeterBinder rProcesses(ProfileService profileService, RProcessEndpoint rProcessEndpoint) {

    return registry ->
        runAsSystem(
            () ->
                profileService.getAll().stream()
                    .map(ContainerConfig::getName)
                    .forEach(
                        environment ->
                            Gauge.builder(
                                    "rserve.processes.current",
                                    () -> rProcessEndpoint.countRServeProcesses(environment))
                                .tag("environment", environment)
                                .description(
                                    "Current number of RServe processes on the R environment")
                                .register(registry)));
  }
}
