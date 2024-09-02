package org.molgenis.armadillo.storage;

import java.io.InputStream;
import java.util.function.Consumer;

public class ArmadilloInputStreamConsumer implements Consumer<InputStream> {
  Consumer<InputStream> inputStreamConsumer;
  long size;

  public ArmadilloInputStreamConsumer(Consumer<InputStream> consumer) {
    inputStreamConsumer = consumer;
  }

  @Override
  public void accept(InputStream inputStream) {
    inputStreamConsumer.accept(inputStream);
  }
}
