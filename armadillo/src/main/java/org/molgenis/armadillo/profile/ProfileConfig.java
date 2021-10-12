package org.molgenis.armadillo.profile;

import java.util.concurrent.Executors;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RConnectionFactoryImpl;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.config.RServeConfig;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
public class ProfileConfig {
  @Bean
  @org.molgenis.armadillo.config.annotation.ProfileScope
  public ProfileConfigProps profileConfigProps(DataShieldConfigProps dataShieldConfigProps) {
    return dataShieldConfigProps.getProfiles().stream()
            .filter(it -> it.getName().equals(ActiveProfileNameAccessor.getActiveProfileName()))
            .findFirst()
            .orElseThrow();
  }

  @Bean
  @org.molgenis.armadillo.config.annotation.ProfileScope
  public RConnectionFactory rConnectionFactory(EnvironmentConfigProps environmentConfigProps) {
    return new RConnectionFactoryImpl(environmentConfigProps);
  }

  // N.B. Thou shalt not name they beans "environment"!
  @Bean
  @org.molgenis.armadillo.config.annotation.ProfileScope
  public EnvironmentConfigProps environmentConfigProps(
          ProfileConfigProps profileConfigProps, RServeConfig rServeConfig) {
    return rServeConfig.getEnvironments().stream()
            .filter(it -> it.getName().equals(profileConfigProps.getEnvironment()))
            .findFirst()
            .orElseThrow();
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor(ProfileScope profileScope) {
    return beanFactory -> beanFactory.registerScope("profile", profileScope);
  }

  /**
   * Added TaskExecutor instead of the ExecutorService to copy the request attributes (in particular
   * the profile definition) from the request to the thread executing the R-command.
   */
  @Bean
  public TaskExecutor executorService() {
    TaskExecutorAdapter taskExecutorAdapter =
            new TaskExecutorAdapter(Executors.newCachedThreadPool());
    taskExecutorAdapter.setTaskDecorator(
            runnable -> {
              RequestAttributes context = RequestContextHolder.currentRequestAttributes();
              return () -> {
                try {
                  RequestContextHolder.setRequestAttributes(context);
                  runnable.run();
                } finally {
                  RequestContextHolder.resetRequestAttributes();
                }
              };
            });
    return taskExecutorAdapter;
  }
}

