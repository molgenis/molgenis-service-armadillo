package org.molgenis.datashield;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.molgenis.datashield.DataShieldUtils.serializeExpression;
import static org.obiba.datashield.core.DSMethodType.AGGREGATE;
import static org.obiba.datashield.core.DSMethodType.ASSIGN;
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
import org.mockito.Mock;
import org.molgenis.datashield.command.Commands;
import org.molgenis.datashield.command.DataShieldCommandDTO;
import org.molgenis.datashield.exceptions.DataShieldExpressionException;
import org.molgenis.datashield.service.DataShieldEnvironmentHolder;
import org.molgenis.datashield.service.DataShieldExpressionRewriter;
import org.molgenis.r.model.RPackage;
import org.obiba.datashield.core.DSEnvironment;
import org.obiba.datashield.core.DSMethod;
import org.obiba.datashield.core.impl.PackagedFunctionDSMethod;
import org.obiba.datashield.r.expr.ParseException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPRaw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(DataController.class)
class DataControllerTest {

  @TestConfiguration
  static class PermissionBean {
    @Bean
    public PermissionEvaluator permissionEvaluator() {
      return new DataShieldPermissionEvaluator();
    }
  }

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
  @MockBean private DataShieldExpressionRewriter expressionRewriter;
  @MockBean private Commands commands;
  @MockBean private DataShieldEnvironmentHolder environments;
  @Mock private REXP rexp;
  @Mock private DSEnvironment assignEnvironment;

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
  void getAssignMethods() throws Exception {
    when(environments.getEnvironment(ASSIGN)).thenReturn(assignEnvironment);
    DSMethod method = new PackagedFunctionDSMethod("meanDS", "dsBase::meanDS", "dsBase", "1.2.3");
    when(assignEnvironment.getMethods()).thenReturn(List.of(method));

    mockMvc
        .perform(get("/methods/assign"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("meanDS"))
        .andExpect(jsonPath("$[0].function").value("dsBase::meanDS"))
        .andExpect(jsonPath("$[0].package").value("dsBase"))
        .andExpect(jsonPath("$[0].version").value("1.2.3"));
  }

  @Test
  @WithMockUser
  void getAggregateMethods() throws Exception {
    when(environments.getEnvironment(AGGREGATE)).thenReturn(assignEnvironment);
    DSMethod method = new PackagedFunctionDSMethod("ls", "base::ls", "base", null);
    when(assignEnvironment.getMethods()).thenReturn(List.of(method));

    mockMvc
        .perform(get("/methods/aggregate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("ls"))
        .andExpect(jsonPath("$[0].function").value("base::ls"))
        .andExpect(jsonPath("$[0].package").value("base"))
        .andExpect(jsonPath("$[0].version", nullValue()));
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
  @WithMockUser(username = "henk")
  void testDeleteWorkspace() throws Exception {
    mockMvc.perform(delete("/workspaces/test")).andExpect(status().isOk());

    verify(commands).removeWorkspace("henk/test.RData");
  }

  @Test
  @WithMockUser(username = "henk")
  void testSaveWorkspace() throws Exception {
    when(commands.saveWorkspace("henk/servername:test_dash.RData"))
        .thenReturn(completedFuture(null));

    mockMvc.perform(post("/workspaces/servername:test_dash")).andExpect(status().isCreated());
  }

  @Test
  @WithMockUser
  void testSaveWorkspaceWrongId() throws Exception {
    mockMvc
        .perform(post("/workspaces/)servername:*wrongid-dash"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "saveUserWorkspace.id: Please use only letters, numbers, dashes or underscores"));
  }

  @Test
  @WithMockUser(username = "henk")
  void testLoadWorkspace() throws Exception {
    when(commands.loadUserWorkspace("henk/blah.RData")).thenReturn(completedFuture(null));

    mockMvc.perform(post("/load-workspace?id=blah")).andExpect(status().isOk());
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

  @WithMockUser(roles = {"DIABETES_RESEARCHER", "GECKO_RESEARCHER"})
  @Test
  public void testLoadTibbles() throws Exception {
    when(commands.loadWorkspaces(asList("DIABETES/patient.RData", "GECKO/patient.RData")))
        .thenReturn(completedFuture(null));
    mockMvc
        .perform(post("/load-tables?workspace=DIABETES/patient&workspace=GECKO/patient"))
        .andExpect(status().isOk());
  }

  @WithMockUser(roles = {"DIABETES_RESEARCHER"})
  @Test
  public void testLoadTibblesForbidden() throws Exception {
    mockMvc
        .perform(post("/load-tables?workspace=DIABETES/patient&workspace=GECKO/patient"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(commands);
  }
}
