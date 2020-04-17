package org.molgenis.datashield;

import static java.time.Instant.now;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.molgenis.datashield.command.Commands;
import org.molgenis.datashield.command.DataShieldCommandDTO;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.molgenis.datashield.service.DataShieldExpressionRewriter;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorServiceImpl;
import org.obiba.datashield.r.expr.ParseException;
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
  @MockBean private Commands commands;
  @Mock private RFileInputStream inputStream;
  @Mock private RConnection rConnection;
  @Mock private REXP rexp;

  @Test
  @WithMockUser
  void testGetPackages() throws Exception {
    when(commands.getPackages()).thenReturn(completedFuture(List.of(BASE, DESC)));
    mockMvc
        .perform(get("/packages"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[{\"name\": \"base\"}, {\"name\": \"desc\"}]"));
  }

  @Test
  @WithMockUser
  void getGetTables() throws Exception {
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
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
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT", "datashield.SAMPLE"});

    mockMvc.perform(head("/tables/datashield.PATIENT")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testTableNotFound() throws Exception {
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
    when(rexp.asStrings()).thenReturn(new String[] {});

    mockMvc.perform(head("/tables/datashield.PATIENT")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void getGetSymbols() throws Exception {
    when(commands.evaluate("base::ls()")).thenReturn(completedFuture(rexp));
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
    when(commands.evaluate("base::rm(D)")).thenReturn(completedFuture(null));
    mockMvc.perform(delete("/symbols/D")).andExpect(status().isOk());
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
    when(commands.getLastExecution()).thenReturn(Optional.of(completedFuture(new REXPRaw(bytes))));

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
            .status(Commands.DataShieldCommandStatus.PENDING)
            .expression("expression")
            .id(UUID.randomUUID())
            .withResult(true)
            .build();
    when(commands.getLastCommand()).thenReturn(Optional.of(command));

    mockMvc
        .perform(get("/lastcommand").accept(APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(jsonPath("status").value("PENDING"));
  }

  @Test
  @WithMockUser
  void testSaveWorkspace() throws Exception {
    UUID uuid = new UUID(123, 456);
    when(idGenerator.generateId()).thenReturn(uuid);

    when(commands.saveWorkspace("00000000-0000-007b-0000-0000000001c8/.RData"))
        .thenReturn(completedFuture(null));

    mockMvc
        .perform(post("/save-workspace"))
        .andExpect(status().isOk())
        .andExpect(content().string("00000000-0000-007b-0000-0000000001c8"));
  }

  @Test
  @WithMockUser
  void testLoadWorkspace() throws Exception {
    when(commands.loadWorkspace("00000000-0000-007b-0000-0000000001c8/.RData", ".GlobalEnv"))
        .thenReturn(completedFuture(null));

    mockMvc
        .perform(post("/load-workspace/00000000-0000-007b-0000-0000000001c8"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testExecute() throws Exception {
    String expression = "meanDS(D$age)";
    String rewrittenExpression = "dsBase::meanDS(D$age)";
    when(expressionRewriter.rewriteAggregate(expression)).thenReturn(rewrittenExpression);
    String serializedExpression = serializeExpression(rewrittenExpression);

    when(commands.evaluate(serializedExpression))
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
    when(commands.evaluate("meanDS(D$age)")).thenReturn(completedFuture(new REXPDouble(36.6)));

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
    when(commands.assign("E", rewrittenExpression)).thenReturn(assignment);

    MvcResult result =
        mockMvc.perform(post("/symbols/E").contentType(TEXT_PLAIN).content(expression)).andReturn();

    assignment.complete(null);
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testAssignSyntaxError() throws Exception {
    String expression = "meanDS(D$age";
    doThrow(new DataShieldExpressionException(new ParseException("Missing end bracket")))
        .when(expressionRewriter)
        .rewriteAssign(expression);

    MvcResult mvcResult =
        mockMvc
            .perform(post("/symbols/D").contentType(TEXT_PLAIN).content(expression))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertEquals(
        "Error parsing expression: Missing end bracket",
        mvcResult.getResolvedException().getMessage());
  }

  @Test
  @WithMockUser
  void testAsyncAssignExecutionFails() throws Exception {
    String expression = "meanDS(D$age)";
    doThrow(new DataShieldExpressionException(new ParseException("Missing end bracket")))
        .when(expressionRewriter)
        .rewriteAssign(expression);

    MvcResult mvcResult =
        mockMvc
            .perform(post("/symbols/D").contentType(TEXT_PLAIN).content(expression))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertEquals(
        "Error parsing expression: Missing end bracket",
        mvcResult.getResolvedException().getMessage());
  }

  @Test
  @WithMockUser
  void testExecuteSyntaxError() throws Exception {
    String expression = "meanDS(D$age";
    doThrow(new DataShieldExpressionException(new ParseException("Missing end bracket")))
        .when(expressionRewriter)
        .rewriteAggregate(expression);

    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/execute?async=true")
                    .accept(APPLICATION_OCTET_STREAM)
                    .contentType(TEXT_PLAIN)
                    .content(expression))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertEquals(
        "Error parsing expression: Missing end bracket",
        mvcResult.getResolvedException().getMessage());
  }

  @Test
  @WithMockUser
  void testAssignAsync() throws Exception {
    String expression = "meanDS(D$age)";
    String rewrittenExpression = "dsBase::meanDS(D$age)";
    when(expressionRewriter.rewriteAssign(expression)).thenReturn(rewrittenExpression);

    when(commands.assign("E", rewrittenExpression)).thenReturn(new CompletableFuture<>());

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
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
    when(rexp.asStrings()).thenReturn(new String[] {});

    mockMvc.perform(post("/symbols/D?table=datashield.PATIENT")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void testLoadTable() throws Exception {
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT"});

    when(commands.assign("D", "base::local(datashield.PATIENT, envir = .DSTableEnv)"))
        .thenReturn(completedFuture(null));

    mockMvc.perform(post("/symbols/D?table=datashield.PATIENT")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void testLoadTableWithVariables() throws Exception {
    when(commands.evaluate("base::local(base::ls(.DSTableEnv))")).thenReturn(completedFuture(rexp));
    when(rexp.asStrings()).thenReturn(new String[] {"datashield.PATIENT"});

    when(commands.assign("D", "base::local(datashield.PATIENT[,c(\"age\")], envir = .DSTableEnv)"))
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
    when(commands.loadWorkspace("DIABETES/patient.RData", ".DSTableEnv"))
        .thenReturn(completedFuture(null));
    mockMvc.perform(post("/load-tables/DIABETES/patient")).andExpect(status().isOk());
  }
}
