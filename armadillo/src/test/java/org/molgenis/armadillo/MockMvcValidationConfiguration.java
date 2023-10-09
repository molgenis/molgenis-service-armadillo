package org.molgenis.armadillo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
/**
 * Dummy implementation to test spring error handling mechanism See
 * https://github.com/spring-projects/spring-boot/issues/5574#issuecomment-506282892
 */
public class MockMvcValidationConfiguration {

  private final BasicErrorController errorController;

  public MockMvcValidationConfiguration(BasicErrorController errorController) {
    this.errorController = errorController;
  }

  // add any exceptions/validations/binding problems
  @ExceptionHandler({MethodArgumentNotValidException.class})
  public ResponseEntity defaultErrorHandler(HttpServletRequest request, Exception ex) {
    request.setAttribute("jakarta.servlet.error.request_uri", request.getPathInfo());
    request.setAttribute("jakarta.servlet.error.status_code", 400);
    return errorController.error(request);
  }
}
