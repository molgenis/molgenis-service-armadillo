package org.molgenis.armadillo.info;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RMetrics {
  @Bean
  MeterBinder rProcesses(RProcessEndpoint processes) {
    return registry ->
        Gauge.builder(
                "rserve_processes_current",
                () ->
                    processes.getRProcesses().stream()
                        .filter(it -> it.name().startsWith("Rserve"))
                        .count())
            .description("Current number of RServe processes on the R server")
            .register(registry);
  }
}
