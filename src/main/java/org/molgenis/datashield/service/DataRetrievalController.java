package org.molgenis.datashield.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataRetrievalController {
  @GetMapping("/retrieve")
  public String retrieve() {
    return "test";
  }
}
