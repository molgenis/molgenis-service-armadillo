package org.molgenis.armadillo.info;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RMetrics {

  @Bean
  MeterBinder rProcesses(
      ArmadilloMetadataService armadilloMetadataService, RProcessEndpoint processes) {

    return registry ->
        runAsSystem(
            () ->
                armadilloMetadataService.profileList().stream()
                    .map(ProfileConfig::getName)
                    .forEach(
                        environment ->
                            Gauge.builder(
                                    "rserve.processes.current",
                                    () -> processes.countRServeProcesses(environment))
                                .tag("environment", environment)
                                .description(
                                    "Current number of RServe processes on the R environment")
                                .register(registry)));
  }
}
