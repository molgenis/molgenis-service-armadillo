package org.molgenis.armadillo.controller;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

public class AuditEventValidator {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ArgumentCaptor<AuditApplicationEvent> eventCaptor;

  public AuditEventValidator(
      ApplicationEventPublisher applicationEventPublisher,
      ArgumentCaptor<AuditApplicationEvent> eventCaptor) {
    this.applicationEventPublisher = applicationEventPublisher;
    this.eventCaptor = eventCaptor;
  }

  void validateAuditEvent(AuditEvent expectedEvent) {
    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    final var auditEvent = eventCaptor.getValue().getAuditEvent();
    final var equal = reflectionEquals(auditEvent, expectedEvent);
    if (!equal) {
      System.out.println(auditEvent);
      System.out.println(expectedEvent);
    }
    assertTrue(equal);
  }
}
