package org.molgenis.datashield;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.DataShieldUtils.serializeCommand;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.molgenis.datashield.service.DownloadServiceImpl;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.RConnectionConsumer;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.model.Package;
import org.molgenis.r.model.Table;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorServiceImpl;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPNull;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.IdGenerator;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DataController.class)
class DataControllerTest {

  public static Package BASE =
      Package.builder()
          .setName("base")
          .setVersion("3.6.1")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  public static Package DESC =
      Package.builder()
          .setName("desc")
          .setVersion("1.2.0")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  @Autowired private MockMvc mockMvc;
  @MockBean private DownloadServiceImpl downloadService;
  @MockBean private RExecutorServiceImpl rExecutorService;
  @MockBean private DataShieldSession datashieldSession;
  @MockBean private PackageService packageService;
  @MockBean private StorageService storageService;
  @MockBean private IdGenerator idGenerator;
  @MockBean private ExecutorService executorService;
  @Mock private RFileInputStream inputStream;
  @Mock private RConnection rConnection;

  @SuppressWarnings({"unchecked"})
  @Test
  @WithMockUser
  void testLoad() throws Exception {
    String assignSymbol = "D";
    Table table = mock(Table.class);
    ResponseEntity<Resource> response = mock(ResponseEntity.class);
    Resource resource = mock(Resource.class);
    when(response.getBody()).thenReturn(resource);
    when(downloadService.getMetadata("project.patients")).thenReturn(table);
    when(downloadService.download(table)).thenReturn(response);
    mockDatashieldExecuteSessionConsumer();

    mockMvc.perform(get("/load/project.patients/D")).andExpect(status().isOk());

    assertAll(
        () -> verify(downloadService).getMetadata("project.patients"),
        () -> verify(downloadService).download(table),
        () -> verify(rExecutorService).assign(resource, assignSymbol, table, rConnection));
  }

  @Test
  @WithMockUser
  void testGetPackages() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(packageService.getInstalledPackages(rConnection)).thenReturn(List.of(BASE, DESC));
    mockMvc
        .perform(get("/packages"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[{\"name\": \"base\"}, {\"name\": \"desc\"}]"));
  }

  @Test
  @WithMockUser
  void testGetLastResultNoResult() throws Exception {
    MvcResult result = mockMvc.perform(get("/lastresult").accept(APPLICATION_JSON)).andReturn();
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void testGetLastResultJson() throws Exception {
    when(datashieldSession.getLastExecution()).thenReturn(completedFuture(new REXPDouble(12.34)));

    MvcResult result = mockMvc.perform(get("/lastresult").accept(APPLICATION_JSON)).andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[12.34]"));
  }

  @Test
  @WithMockUser
  void testGetLastResultRaw() throws Exception {
    byte[] bytes = {0x0, 0x1, 0x2};
    when(datashieldSession.getLastExecution()).thenReturn(completedFuture(new REXPRaw(bytes)));

    MvcResult result =
        mockMvc.perform(get("/lastresult").accept(APPLICATION_OCTET_STREAM)).andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
        .andExpect(content().bytes(bytes));
  }

  @Test
  @WithMockUser
  void testExists() throws Exception {
    when(downloadService.metadataExists("project.patients")).thenReturn(true);
    mockMvc
        .perform(get("/exists/project.patients"))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  @WithMockUser
  void testSaveWorkspace() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    UUID uuid = new UUID(123, 456);
    when(idGenerator.generateId()).thenReturn(uuid);
    when(rConnection.openFile(".RData")).thenReturn(inputStream);
    doAnswer(
            invocation -> {
              Consumer<InputStream> consumer = invocation.getArgument(1);
              consumer.accept(inputStream);
              return null;
            })
        .when(rExecutorService)
        .saveWorkspace(eq(rConnection), any());

    mockMvc
        .perform(post("/save-workspace"))
        .andExpect(status().isOk())
        .andExpect(content().string("00000000-0000-007b-0000-0000000001c8"));

    verify(storageService)
        .save(inputStream, "00000000-0000-007b-0000-0000000001c8/.RData", APPLICATION_OCTET_STREAM);
  }

  @Test
  @WithMockUser
  void testLoadWorkspace() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(rConnection.openFile(".RData")).thenReturn(inputStream);
    when(storageService.load("00000000-0000-007b-0000-0000000001c8/.RData"))
        .thenReturn(inputStream);

    mockMvc
        .perform(post("/load-workspace/00000000-0000-007b-0000-0000000001c8"))
        .andExpect(status().isOk());

    verify(rExecutorService).loadWorkspace(eq(rConnection), any());
  }

  @SuppressWarnings({"unchecked"})
  @Test
  @WithMockUser
  void testLoadFailed() throws Exception {
    String assignSymbol = "D";
    Table table = mock(Table.class);
    ResponseEntity<Resource> response = mock(ResponseEntity.class);
    Resource resource = mock(Resource.class);
    when(response.getBody()).thenReturn(resource);
    when(downloadService.getMetadata("project.patients")).thenReturn(table);
    when(downloadService.download(table)).thenReturn(response);
    mockDatashieldExecuteSessionConsumer();
    RExecutionException exception = new RExecutionException(new IOException("test"));
    when(rExecutorService.assign(resource, assignSymbol, table, rConnection)).thenThrow(exception);

    assertThatThrownBy(() -> mockMvc.perform(get("/load/project.patients/D")))
        .hasCauseReference(exception);

    assertAll(
        () -> verify(downloadService).getMetadata("project.patients"),
        () -> verify(downloadService).download(table),
        () -> verify(rExecutorService).assign(resource, assignSymbol, table, rConnection));
  }

  @Test
  @WithMockUser
  void testExecuteAsync() throws Exception {
    mockDatashieldScheduleSessionConsumer();
    when(rExecutorService.execute("dsMean(D$age)", rConnection)).thenReturn(new REXPDouble(36.6));

    MvcResult result =
        mockMvc
            .perform(
                post("/execute?async=true")
                    .contentType(TEXT_PLAIN)
                    .content("dsMean(D$age)")
                    .accept(APPLICATION_JSON))
            .andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/lastresult"))
        .andExpect(content().string(""));
  }

  @Test
  @WithMockUser
  void testExecuteDoubleResult() throws Exception {
    mockDatashieldScheduleSessionConsumer();
    when(rExecutorService.execute("dsMean(D$age)", rConnection)).thenReturn(new REXPDouble(36.6));

    MvcResult result =
        mockMvc
            .perform(
                post("/execute")
                    .contentType(TEXT_PLAIN)
                    .content("dsMean(D$age)")
                    .accept(APPLICATION_JSON))
            .andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().string("[36.6]"));
  }

  @Test
  @WithMockUser
  void testExecuteNullResult() throws Exception {
    mockDatashieldScheduleSessionConsumer();
    when(rExecutorService.execute("install.package('dsBase')", rConnection))
        .thenReturn(new REXPNull());

    MvcResult result =
        mockMvc
            .perform(
                post("/execute")
                    .contentType(TEXT_PLAIN)
                    .accept(APPLICATION_JSON)
                    .content("install.package('dsBase')"))
            .andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().string(""));

    verify(rExecutorService).execute("install.package('dsBase')", rConnection);
  }

  @Test
  @WithMockUser
  void testExecuteRawResult() throws Exception {
    mockDatashieldScheduleSessionConsumer();
    String serializedCmd = serializeCommand("print(\"raw response\")");

    when(rExecutorService.execute(serializedCmd, rConnection)).thenReturn(new REXPRaw(new byte[0]));

    mockMvc
        .perform(
            post("/execute")
                .accept(APPLICATION_OCTET_STREAM)
                .contentType(TEXT_PLAIN)
                .content("print(\"raw response\")"))
        .andExpect(status().isOk());

    verify(rExecutorService).execute(serializedCmd, rConnection);
  }

  @SuppressWarnings("unchecked")
  private void mockDatashieldExecuteSessionConsumer() {
    doAnswer(answer -> answer.<RConnectionConsumer<String>>getArgument(0).accept(rConnection))
        .when(datashieldSession)
        .execute(any(RConnectionConsumer.class));
  }

  @SuppressWarnings("unchecked")
  private void mockDatashieldScheduleSessionConsumer() {
    doAnswer(
            answer ->
                completedFuture(
                    answer.<RConnectionConsumer<REXP>>getArgument(0).accept(rConnection)))
        .when(datashieldSession)
        .schedule(any(RConnectionConsumer.class));
  }
}
