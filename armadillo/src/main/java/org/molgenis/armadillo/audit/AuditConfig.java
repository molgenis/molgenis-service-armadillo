package org.molgenis.armadillo.audit;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditConfig {

  @Bean
  public AuditLogger auditLogger() {
    return new AuditLogger();
  }

  @Bean
  public AuditEventRepository auditEventRepository() {
    return new InMemoryAuditEventRepository();
  }

  @Bean
  public AuditListener auditListener() {
    return new AuditListener(auditEventRepository());
  }
}
