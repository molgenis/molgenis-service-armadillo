package org.molgenis.connection.flower.service;

import java.io.InputStream;

public class FlowerExecutorService {
  public FlowerExecutorService(String containerName) {}

  // connection between armadillo and connection part of application for flower
  public void loadData(String container, String fileName, InputStream data, long size) {

    // TODO: how do we pass size while still adhering to interface?
  }
}
