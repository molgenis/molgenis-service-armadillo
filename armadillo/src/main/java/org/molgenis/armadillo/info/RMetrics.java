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
        Gauge.builder("rserve.processes.current", processes::countRServeProcesses)
            .description("Current number of RServe processes on the R server")
            .register(registry);
  }
}
