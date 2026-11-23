package org.molgenis.armadillo.service;

import java.io.IOException;

public interface RebootScriptRunner {
  void runRebootScript(String... args) throws IOException;
}
