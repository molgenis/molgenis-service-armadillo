package org.molgenis.armadillo.info;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.config.RServeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RMetrics {

  @Bean
  MeterBinder rProcesses(RServeConfig rServeConfig, RProcessEndpoint processes) {

    return registry ->
        rServeConfig.getEnvironments().stream()
            .map(EnvironmentConfigProps::getName)
            .forEach(
                environment -> Gauge.builder(
                        "rserve.processes.current." + environment,
                        () -> processes.countRServeProcesses(environment))
                    .description(
                        "Current number of RServe processes on the R environment [ "
                            + environment
                            + " ]")
                    .register(registry));
  }
}
