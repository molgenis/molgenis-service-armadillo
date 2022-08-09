package org.molgenis.armadillo.info;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RMetrics {

  @Bean
  MeterBinder rProcesses(DataShieldConfigProps rServeConfig, RProcessEndpoint processes) {

    return registry ->
        rServeConfig.getProfiles().stream()
            .map(EnvironmentConfigProps::getName)
            .forEach(
                environment ->
                    Gauge.builder(
                            "rserve.processes.current",
                            () -> processes.countRServeProcesses(environment))
                        .tag("environment", environment)
                        .description("Current number of RServe processes on the R environment")
                        .register(registry));
  }
}
