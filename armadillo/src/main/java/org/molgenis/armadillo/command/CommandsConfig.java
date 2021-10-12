package org.molgenis.armadillo.command;

import org.molgenis.armadillo.profile.ActiveProfileNameAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executors;

import static org.springframework.security.core.context.SecurityContextHolder.*;

@Configuration
public class CommandsConfig {

    /**
     * Added TaskExecutor instead of the ExecutorService to copy the request attributes (in particular
     * the profile definition) from the request to the thread executing the R-command.
     */
    @Bean
    public TaskExecutor executorService() {
        TaskExecutorAdapter taskExecutorAdapter = new TaskExecutorAdapter(Executors.newCachedThreadPool());
        taskExecutorAdapter.setTaskDecorator(
                runnable -> {
                    final SecurityContext securityContext = SecurityContextHolder.getContext();
                    final String profile = ActiveProfileNameAccessor.getActiveProfileName();
                    return () -> {
                        final SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
                        try {
                            SecurityContextHolder.setContext(securityContext);
                            ActiveProfileNameAccessor.setActiveProfileThreadLocal(profile);
                            runnable.run();
                        } finally {
                            SecurityContext emptyContext = createEmptyContext();
                            if (emptyContext.equals(originalSecurityContext)) {
                                clearContext();
                            } else {
                                setContext(originalSecurityContext);
                            }
                            ActiveProfileNameAccessor.resetActiveProfileThreadLocal();
                        }
                    };
                });
        return taskExecutorAdapter;
    }
}
