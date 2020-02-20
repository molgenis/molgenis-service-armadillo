package org.molgenis.datashield;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.datashield.exceptions.DatashieldRequestFailedException;
import org.molgenis.datashield.r.RConnectionConsumer;
import org.molgenis.datashield.r.RDatashieldSession;
import org.molgenis.datashield.service.DownloadServiceImpl;
import org.molgenis.datashield.service.RExecutorServiceImpl;
import org.molgenis.datashield.service.model.Table;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DataController.class)
class DataControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private DownloadServiceImpl downloadService;
  @MockBean private RExecutorServiceImpl executorService;
  @MockBean private RDatashieldSession datashieldSession;

  @SuppressWarnings({"unchecked"})
  @Test
  @WithMockUser
  void testLoad() throws Exception {
    Table table = mock(Table.class);
    ResponseEntity<Resource> response = mock(ResponseEntity.class);
    Resource resource = mock(Resource.class);
    InputStream csvStream = mock(InputStream.class);
    when(response.getBody()).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(csvStream);
    when(downloadService.getMetadata("project.patients")).thenReturn(table);
    when(downloadService.download(table)).thenReturn(response);
    RConnection rConnection = mockDatashieldSessionConsumer();

    mockMvc.perform(get("/load/project.patients")).andExpect(status().isOk());

    assertAll(
        () -> verify(downloadService).getMetadata("project.patients"),
        () -> verify(downloadService).download(table),
        () -> verify(executorService).assign(csvStream, table, rConnection));
  }

  @SuppressWarnings({"unchecked"})
  @Test
  @WithMockUser
  void testLoadFailed() throws Exception {
    Table table = mock(Table.class);
    ResponseEntity<Resource> response = mock(ResponseEntity.class);
    Resource resource = mock(Resource.class);
    InputStream csvStream = mock(InputStream.class);
    when(response.getBody()).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(csvStream);
    when(downloadService.getMetadata("project.patients")).thenReturn(table);
    when(downloadService.download(table)).thenReturn(response);
    RConnection rConnection = mockDatashieldSessionConsumer();
    IOException exception = new IOException("test");
    when(executorService.assign(csvStream, table, rConnection)).thenThrow(exception);

    assertThatThrownBy(() -> mockMvc.perform(get("/load/project.patients")))
        .hasCause(new DatashieldRequestFailedException("test", exception));

    assertAll(
        () -> verify(downloadService).getMetadata("project.patients"),
        () -> verify(downloadService).download(table),
        () -> verify(executorService).assign(csvStream, table, rConnection));
  }

  @Test
  @WithMockUser
  void testExecute() throws Exception {
    RConnection rConnection = mockDatashieldSessionConsumer();

    mockMvc
        .perform(post("/execute").contentType(MediaType.TEXT_PLAIN).content("mean(age)"))
        .andExpect(status().isOk());

    verify(executorService).execute("mean(age)", rConnection);
  }

  @SuppressWarnings("unchecked")
  private RConnection mockDatashieldSessionConsumer()
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
