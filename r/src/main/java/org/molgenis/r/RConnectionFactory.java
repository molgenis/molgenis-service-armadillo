package org.molgenis.r;

import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface RConnectionFactory {
  @Retryable(
      value = {ConnectionCreationFailedException.class},
      maxAttempts = 5,
      backoff = @Backoff(delay = 1000))
  RConnection retryCreateConnection();

  RConnection createConnection();
}
