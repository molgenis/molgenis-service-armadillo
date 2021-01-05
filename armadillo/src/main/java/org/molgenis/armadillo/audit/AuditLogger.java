package org.molgenis.armadillo.audit;

import static net.logstash.logback.marker.Markers.*;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AbstractAuditListener;

public class AuditLogger extends AbstractAuditListener {
  private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);

  @Override
  protected void onAuditEvent(AuditEvent event) {
    if (logger.isInfoEnabled()) {
      logger.info(
          append("timestamp", event.getTimestamp().toString())
              .and(append("principal", event.getPrincipal()))
              .and(append("type", event.getType()))
              .and(appendEntries(Map.of("data", event.getData()))),
          event.toString());
    }
  }
}
