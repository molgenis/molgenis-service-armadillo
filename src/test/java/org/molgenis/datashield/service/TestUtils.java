package org.molgenis.datashield.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import org.molgenis.api.metadata.model.EntityType;
import org.molgenis.datashield.r.RConnectionConsumer;
import org.molgenis.datashield.r.RDatashieldSession;
import org.rosuda.REngine.Rserve.RConnection;

public class TestUtils {
  public static EntityType getEntityType(String jsonFileName) {
    return new Gson()
        .fromJson(
            new InputStreamReader(TestUtils.class.getResourceAsStream(jsonFileName), UTF_8),
            EntityType.class);
  }

  @SuppressWarnings("unchecked")
  public static RConnection mockDatashieldSessionConsumer(RDatashieldSession datashieldSession)
      throws org.rosuda.REngine.Rserve.RserveException, org.rosuda.REngine.REXPMismatchException {
    RConnection rConnection = mock(RConnection.class);
    doAnswer(
            answer -> {
              RConnectionConsumer<String> consumer = answer.getArgument(0);
              return consumer.accept(rConnection);
            })
        .when(datashieldSession)
        .execute(any(RConnectionConsumer.class));
    return rConnection;
  }
}
