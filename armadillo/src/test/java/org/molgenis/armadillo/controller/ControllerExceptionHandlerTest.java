package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ControllerExceptionHandlerTest {

  @Test
  void handleConnectionCreationFailed_returns503() {
    ResponseEntity<String> response =
        new ControllerExceptionHandler().handleConnectionCreationFailed();

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
  }
}
