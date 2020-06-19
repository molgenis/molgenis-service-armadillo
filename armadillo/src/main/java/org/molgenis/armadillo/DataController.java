package org.molgenis.armadillo;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.molgenis.armadillo.ArmadilloUtils.TABLE_ENV;
import static org.molgenis.armadillo.ArmadilloUtils.getLastCommandLocation;
import static org.molgenis.armadillo.ArmadilloUtils.serializeExpression;
import static org.molgenis.r.Formatter.stringVector;
import static org.obiba.datashield.core.DSMethodType.AGGREGATE;
import static org.obiba.datashield.core.DSMethodType.ASSIGN;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.security.Principal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.armadillo.service.DataShieldEnvironmentHolder;
import org.molgenis.armadillo.service.ExpressionRewriter;
import org.molgenis.r.model.RPackage;
import org.obiba.datashield.core.DSMethod;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo", version = "0.1.0"),
    security = {@SecurityRequirement(name = "JSESSIONID"), @SecurityRequirement(name = "http")})
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@RestController
@Validated
public class DataController {

  public static final String SYMBOL_RE = "\\p{Alnum}[\\w.]*";
  public static final String SYMBOL_CSV_RE = "\\p{Alnum}[\\w.]*(,\\p{Alnum}[\\w.]*)*";
  public static final String WORKSPACE_OBJECTNAME_TEMPLATE = "%s/%s.RData";
  public static final String WORKSPACE_ID_FORMAT_REGEX = "[\\w-:]+";

  private final ExpressionRewriter expressionRewriter;
  private final Commands commands;
  private final DataShieldEnvironmentHolder environments;

  public DataController(
      ExpressionRewriter expressionRewriter,
      Commands commands,
      DataShieldEnvironmentHolder environments) {
    this.expressionRewriter = expressionRewriter;
    this.commands = commands;
    this.environments = environments;
  }

  @Operation(summary = "Get R packages", description = "Get all installed R packages.")
  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<RPackage> getPackages() throws ExecutionException, InterruptedException {
    return commands.getPackages().get();
  }

  @Operation(
      summary = "Get available tables",
      description =
          "Return a list of (fully qualified) table identifiers available for DataSHIELD operations")
  @GetMapping(value = "/tables", produces = APPLICATION_JSON_VALUE)
  public List<String> getTables()
      throws ExecutionException, InterruptedException, REXPMismatchException {
    final String command = format("base::local(base::ls(%s))", TABLE_ENV);
    REXP result = commands.evaluate(command).get();
    return asList(result.asStrings());
  }

  @Operation(
      summary = "Check table existence",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "The table exists and is available for DataSHIELD operations")
      })
  @RequestMapping(value = "/tables/{tableId}", method = HEAD)
  public ResponseEntity<Void> tableExists(@PathVariable String tableId)
      throws InterruptedException, ExecutionException, REXPMismatchException {
    return getTables().contains(tableId) ? ok().build() : notFound().build();
  }

  @Operation(summary = "Get assigned symbols")
  @GetMapping(value = "/symbols", produces = APPLICATION_JSON_VALUE)
  public List<String> getSymbols()
      throws ExecutionException, InterruptedException, REXPMismatchException {
    REXP result = commands.evaluate("base::ls()").get();
    return asList(result.asStrings());
  }

  @Operation(
      summary = "Remove symbol",
      description = "Removes a symbol, making the assigned data inaccessible")
  @DeleteMapping(value = "/symbols/{symbol}")
  public void removeSymbol(@Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol)
      throws ExecutionException, InterruptedException {
    String command = format("base::rm(%s)", symbol);
    commands.evaluate(command).get();
  }

  @Operation(
      summary = "Load table",
      description = "Copy variables of a table into a symbol",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "/load-table")
  public ResponseEntity<Void> loadTable(
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String symbol,
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String table,
      @Valid @Pattern(regexp = SYMBOL_CSV_RE) @RequestParam(required = false) String variables)
      throws InterruptedException, ExecutionException, REXPMismatchException {
    if (!getTables().contains(table)) {
      return notFound().build();
    }
    String expression = table;
    if (variables != null) {
      String[] split = variables.split(",");
      expression = format("%s[,%s]", table, stringVector(split));
    }
    expression = format("base::local(%s, envir = %s)", expression, TABLE_ENV);
    commands.assign(symbol, expression).get();
    return ok().build();
  }

  @Operation(
      summary = "Assign symbol",
      description = "Assign the result of the evaluation of an expression to a symbol")
  @PostMapping(value = "/symbols/{symbol}", consumes = TEXT_PLAIN_VALUE)
  public CompletableFuture<ResponseEntity<Void>> assignSymbol(
      @Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol,
      @RequestBody String expression,
      @RequestParam(defaultValue = "false") boolean async) {
    String rewrittenExpression = expressionRewriter.rewriteAssign(expression);
    CompletableFuture<Void> result = commands.assign(symbol, rewrittenExpression);
    return async
        ? completedFuture(created(getLastCommandLocation()).body(null))
        : result
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  @Operation(summary = "Execute expression")
  @PostMapping(value = "/execute", consumes = TEXT_PLAIN_VALUE)
  public CompletableFuture<ResponseEntity<byte[]>> execute(
      @RequestBody String expression,
      @Parameter(description = "Indicates if the expression should be executed asynchronously")
          @RequestParam(defaultValue = "false")
          boolean async) {
    String rewrittenExpression =
        serializeExpression(expressionRewriter.rewriteAggregate(expression));
    CompletableFuture<REXP> result = commands.evaluate(rewrittenExpression);
    return async
        ? completedFuture(created(getLastCommandLocation()).body(null))
        : result
            .thenApply(ArmadilloUtils::createRawResponse)
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  @Operation(summary = "Get last command")
  @GetMapping(value = "/lastcommand", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ArmadilloCommandDTO> getLastCommand() {
    return ResponseEntity.of(commands.getLastCommand());
  }

  @Operation(summary = "Get last result")
  @GetMapping(value = "/lastresult", produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(OK)
  public CompletableFuture<ResponseEntity<byte[]>> lastResult() {
    return commands
        .getLastExecution()
        .map(
            execution ->
                execution
                    .thenApply(ArmadilloUtils::createRawResponse)
                    .thenApply(ResponseEntity::ok)
                    .exceptionally(ex -> notFound().build()))
        .orElse(completedFuture(notFound().build()));
  }

  @Operation(
      summary = "Debug a command",
      description = "Debugs a command, bypassing DataSHIELD's security checks. Admin use only.")
  @PreAuthorize("hasRole('ROLE_SU')")
  @PostMapping(value = "/debug", consumes = TEXT_PLAIN_VALUE, produces = APPLICATION_JSON_VALUE)
  public Object debug(@RequestBody String expression)
      throws ExecutionException, InterruptedException, REXPMismatchException {
    return commands.evaluate(expression).get().asNativeJavaObject();
  }

  @Operation(summary = "Get available assign methods")
  @GetMapping(value = "/methods/assign", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAssignMethods() {
    return environments.getEnvironment(ASSIGN).getMethods();
  }

  @Operation(summary = "Get available aggregate methods")
  @GetMapping(value = "/methods/aggregate", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAggregateMethods() {
    return environments.getEnvironment(AGGREGATE).getMethods();
  }

  @Operation(summary = "Get user workspaces")
  @GetMapping(value = "/workspaces", produces = APPLICATION_JSON_VALUE)
  public List<Workspace> getWorkspaces(Principal principal) {
    return commands.listWorkspaces(principal.getName() + "/");
  }

  @Operation(
      summary = "Delete user workspace",
      responses = {
        @ApiResponse(responseCode = "200", description = "Workspace was removed or did not exist.")
      })
  @DeleteMapping(value = "/workspaces/{id}")
  @ResponseStatus(OK)
  public void removeWorkspace(@PathVariable String id, Principal principal) {
    String objectName = format(WORKSPACE_OBJECTNAME_TEMPLATE, principal.getName(), id);
    commands.removeWorkspace(objectName);
  }

  @Operation(summary = "Save user workspace")
  @PostMapping(value = "/workspaces/{id}", produces = TEXT_PLAIN_VALUE)
  @ResponseStatus(CREATED)
  public void saveUserWorkspace(
      @Pattern(
              regexp = WORKSPACE_ID_FORMAT_REGEX,
              message = "Please use only letters, numbers, dashes or underscores")
          @PathVariable
          String id,
      Principal principal)
      throws ExecutionException, InterruptedException {
    String objectName = format(WORKSPACE_OBJECTNAME_TEMPLATE, principal.getName(), id);
    commands.saveWorkspace(objectName).get();
  }

  @Operation(
      summary = "Load shared workspaces",
      description =
          "Make tables from shared workspaces available for assignment. You need load permission on the workspaces.")
  @PreAuthorize("hasPermission(#workspace, 'Workspace', 'load')")
  @PostMapping(value = "/load-tables")
  public void loadTables(@RequestParam List<String> workspace)
      throws ExecutionException, InterruptedException {
    commands.loadWorkspaces(workspace.stream().map(it -> it + ".RData").collect(toList())).get();
  }

  @Operation(summary = "Load user workspace")
  @PostMapping(value = "/load-workspace")
  public void loadUserWorkspace(@RequestParam String id, Principal principal)
      throws ExecutionException, InterruptedException {
    String objectName = format(WORKSPACE_OBJECTNAME_TEMPLATE, principal.getName(), id);
    commands.loadUserWorkspace(objectName).get();
  }
}
