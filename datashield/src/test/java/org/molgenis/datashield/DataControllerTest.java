package org.molgenis.datashield;

import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.DataShieldUtils.serializeExpression;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.molgenis.datashield.pojo.DataShieldCommand.DataShieldCommandStatus;
import org.molgenis.datashield.pojo.DataShieldCommandDTO;
import org.molgenis.datashield.service.DataShieldExpressionRewriter;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.RConnectionConsumer;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorServiceImpl;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPRaw;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.IdGenerator;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DataController.class)
class DataControllerTest {

  public static RPackage BASE =
      RPackage.builder()
          .setName("base")
          .setVersion("3.6.1")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  public static RPackage DESC =
      RPackage.builder()
          .setName("desc")
          .setVersion("1.2.0")
          .setBuilt("3.6.1")
          .setLibPath("/usr/local/lib/R/site-library")
          .build();

  @Autowired private MockMvc mockMvc;
  @MockBean private RExecutorServiceImpl rExecutorService;
  @MockBean private DataShieldSession datashieldSession;
  @MockBean private PackageService packageService;
  @MockBean private StorageService storageService;
  @MockBean private IdGenerator idGenerator;
  @MockBean private DataShieldExpressionRewriter expressionRewriter;
  @Mock private RFileInputStream inputStream;
  @Mock private RConnection rConnection;
  @Mock private REXP rexp;

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
  void getGetTables() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT", "datashield.SAMPLE"});

    mockMvc
        .perform(get("/tables"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"datashield.PATIENT\",\"datashield.SAMPLE\"]"));
  }

  @Test
  @WithMockUser
  void testTableExists() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT", "datashield.SAMPLE"});

    mockMvc.perform(head("/tables/datashield.PATIENT")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testTableNotFound() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {});

    mockMvc.perform(head("/tables/datashield.PATIENT")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void getGetSymbols() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(rExecutorService.execute("base::ls()", rConnection)).thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {"D"});

    mockMvc
        .perform(get("/symbols"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"D\"]"));
  }

  @Test
  @WithMockUser
  void deleteSymbol() throws Exception {
    mockDatashieldExecuteSessionConsumer();

    mockMvc.perform(delete("/symbols/D")).andExpect(status().isOk());

    verify(rExecutorService).execute("base::rm(D)", rConnection);
  }

  @Test
  @WithMockUser
  void testGetLastResultNoResult() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/lastresult").accept(APPLICATION_OCTET_STREAM)).andReturn();
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void testGetLastResult() throws Exception {
    byte[] bytes = {0x0, 0x1, 0x2};
    when(datashieldSession.getLastExecution())
        .thenReturn(Optional.of(completedFuture(new REXPRaw(bytes))));

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
  void testGetLastCommandNotFound() throws Exception {
    mockMvc.perform(get("/lastcommand").accept(APPLICATION_JSON)).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void testGetLastCommand() throws Exception {
    DataShieldCommandDTO command =
        DataShieldCommandDTO.builder()
            .createDate(now())
            .status(DataShieldCommandStatus.PENDING)
            .expression("expression")
            .id(UUID.randomUUID())
            .withResult(true)
            .build();
    when(datashieldSession.getLastCommand()).thenReturn(Optional.of(command));

    mockMvc
        .perform(get("/lastcommand").accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("status").value("PENDING"));
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

    verify(rExecutorService).loadWorkspace(eq(rConnection), any(), eq(".GlobalEnv"));
  }

  @Test
  @WithMockUser
  void testExecute() throws Exception {
    String expression = "meanDS(D$age)";
    String rewrittenExpression = "dsBase::meanDS(D$age)";
    when(expressionRewriter.rewriteAggregate(expression)).thenReturn(rewrittenExpression);
    String serializedExpression = serializeExpression(rewrittenExpression);

    when(datashieldSession.schedule(serializedExpression))
        .thenReturn(completedFuture(new REXPRaw(new byte[0])));

    mockMvc
        .perform(
            post("/execute")
                .accept(APPLICATION_OCTET_STREAM)
                .contentType(TEXT_PLAIN)
                .content(expression))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testExecuteAsync() throws Exception {
    when(datashieldSession.schedule("meanDS(D$age)"))
        .thenReturn(completedFuture(new REXPDouble(36.6)));

    MvcResult result =
        mockMvc
            .perform(
                post("/execute?async=true")
                    .contentType(TEXT_PLAIN)
                    .content("meanDS(D$age)")
                    .accept(APPLICATION_OCTET_STREAM))
            .andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/lastcommand"))
        .andExpect(content().string(""));
  }

  @Test
  @WithMockUser
  void testAssign() throws Exception {
    String expression = "meanDS(D$age)";
    String rewrittenExpression = "dsBase::meanDS(D$age)";
    when(expressionRewriter.rewriteAssign(expression)).thenReturn(rewrittenExpression);

    CompletableFuture<Void> assignment = new CompletableFuture<>();
    when(datashieldSession.assign("E", rewrittenExpression)).thenReturn(assignment);

    MvcResult result =
        mockMvc
            .perform(
                post("/symbols/E")
                    .accept(APPLICATION_OCTET_STREAM)
                    .contentType(TEXT_PLAIN)
                    .content(expression))
            .andReturn();

    assignment.complete(null);
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testAssignAsync() throws Exception {
    String expression = "meanDS(D$age)";
    String rewrittenExpression = "dsBase::meanDS(D$age)";
    when(expressionRewriter.rewriteAssign(expression)).thenReturn(rewrittenExpression);

    when(datashieldSession.assign("E", rewrittenExpression)).thenReturn(new CompletableFuture<>());

    MvcResult result =
        mockMvc
            .perform(
                post("/symbols/E?async=true")
                    .contentType(TEXT_PLAIN)
                    .content("meanDS(D$age)")
                    .accept(APPLICATION_OCTET_STREAM))
            .andReturn();
    mockMvc
        .perform(asyncDispatch(result))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/lastcommand"))
        .andExpect(content().string(""));
  }

  @Test
  @WithMockUser
  void testLoadTableDoesNotExist() throws Exception {
    mockDatashieldExecuteSessionConsumer();

    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {});

    mockMvc.perform(post("/symbols/D?table=datashield.PATIENT")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void testLoadTable() throws Exception {
    mockDatashieldExecuteSessionConsumer();

    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT"});

    when(datashieldSession.assign("D", "base::local(datashield.PATIENT, envir = .DSTableEnv)"))
        .thenReturn(completedFuture(null));

    mockMvc.perform(post("/symbols/D?table=datashield.PATIENT")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testLoadTableWithVariables() throws Exception {
    mockDatashieldExecuteSessionConsumer();

    when(rExecutorService.execute("base::local(base::ls(.DSTableEnv))", rConnection))
        .thenReturn(rexp);
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT"});

    when(datashieldSession.assign(
            "D", "base::local(datashield.PATIENT[,c(\"age\")], envir = .DSTableEnv)"))
        .thenReturn(completedFuture(null));

    mockMvc
        .perform(post("/symbols/D?table=datashield.PATIENT&variables=age"))
        .andExpect(status().isOk());
  }

  @WithMockUser
  @Test
  public void testLoadTibblesNotAResearcher() throws Exception {
    mockMvc.perform(post("/load-tables/DIABETES/patient")).andExpect(status().isForbidden());
  }

  @WithMockUser(roles = {"DIABETES_RESEARCHER"})
  @Test
  public void testLoadTibbles() throws Exception {
    mockDatashieldExecuteSessionConsumer();
    when(storageService.load("DIABETES/patient.RData")).thenReturn(inputStream);

    mockMvc.perform(post("/load-tables/DIABETES/patient")).andExpect(status().isOk());

    verify(rExecutorService).loadWorkspace(eq(rConnection), any(), eq(".DSTableEnv"));
  }

  @SuppressWarnings("unchecked")
  private void mockDatashieldExecuteSessionConsumer() {
    doAnswer(answer -> answer.<RConnectionConsumer<String>>getArgument(0).accept(rConnection))
        .when(datashieldSession)
        .execute(any(RConnectionConsumer.class));
  }
}
