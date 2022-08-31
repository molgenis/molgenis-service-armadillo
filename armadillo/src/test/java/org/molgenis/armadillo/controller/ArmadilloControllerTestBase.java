package org.molgenis.armadillo.controller;

import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@Import(AuditEventPublisher.class)
@WebMvcTest
public class ArmadilloControllerTestBase {

  @Autowired protected MockMvc mockMvc;
  @Autowired AuditEventPublisher auditEventPublisher;

  @MockBean protected ApplicationEventPublisher applicationEventPublisher;

  @Mock(lenient = true)
  protected Clock clock;

  protected final Instant instant = Instant.now();
  protected final MockHttpSession session = new MockHttpSession();
  @Captor protected ArgumentCaptor<AuditApplicationEvent> eventCaptor;
  protected String sessionId;
  protected AuditEventValidator auditEventValidator;

  @BeforeEach
  public void setup() {
    auditEventValidator = new AuditEventValidator(applicationEventPublisher, eventCaptor);
    auditEventPublisher.setClock(clock);
    auditEventPublisher.setApplicationEventPublisher(applicationEventPublisher);
    when(clock.instant()).thenReturn(instant);
    sessionId = session.changeSessionId();
  }
}
