package org.molgenis.armadillo.command;

import static org.springframework.security.core.context.SecurityContextHolder.*;

import java.util.concurrent.Executors;
import org.molgenis.armadillo.container.ActiveContainerNameAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class CommandsConfig {

  /**
   * Added TaskExecutor instead of the ExecutorService to copy the request attributes (in particular
   */
  @Bean
  @Primary
  public TaskExecutor executorService() {
    TaskExecutorAdapter taskExecutorAdapter =
        new TaskExecutorAdapter(Executors.newCachedThreadPool());
    taskExecutorAdapter.setTaskDecorator(
        runnable -> {
          // this runs in the calling thread
          final SecurityContext securityContext = SecurityContextHolder.getContext();
          final String container = ActiveContainerNameAccessor.getActiveContainerName();
          return () -> {
            // this runs in the task thread
            final SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
            try {
              SecurityContextHolder.setContext(securityContext);
              ActiveContainerNameAccessor.setActiveContainerName(container);
              runnable.run();
            } finally {
              SecurityContext emptyContext = createEmptyContext();
              if (emptyContext.equals(originalSecurityContext)) {
                clearContext();
              } else {
                setContext(originalSecurityContext);
              }
              ActiveContainerNameAccessor.resetActiveContainerName();
            }
          };
        });
    return taskExecutorAdapter;
  }
}
