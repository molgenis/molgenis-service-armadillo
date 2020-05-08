package org.molgenis.armadillo;

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

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<RPackage> getPackages() throws ExecutionException, InterruptedException {
    return commands.getPackages().get();
  }

  /** @return a list of (fully qualified) table identifiers available for DataSHIELD operations. */
  @GetMapping(value = "/tables", produces = APPLICATION_JSON_VALUE)
  public List<String> getTables()
      throws ExecutionException, InterruptedException, REXPMismatchException {
    final String command = format("base::local(base::ls(%s))", TABLE_ENV);
    REXP result = commands.evaluate(command).get();
    return asList(result.asStrings());
  }

  /** @return OK if the the table exists and is available for DataSHIELD operations. */
  @RequestMapping(value = "/tables/{tableId}", method = HEAD)
  public ResponseEntity<Void> tableExists(@PathVariable String tableId)
      throws InterruptedException, ExecutionException, REXPMismatchException {
    return getTables().contains(tableId) ? ok().build() : notFound().build();
  }

  /** @return a list of assigned symbols */
  @GetMapping(value = "/symbols", produces = APPLICATION_JSON_VALUE)
  public List<String> getSymbols()
      throws ExecutionException, InterruptedException, REXPMismatchException {
    REXP result = commands.evaluate("base::ls()").get();
    return asList(result.asStrings());
  }

  /** Removes a symbol, making the assigned data inaccessible */
  @DeleteMapping(value = "/symbols/{symbol}")
  public void removeSymbol(@Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol)
      throws ExecutionException, InterruptedException {
    String command = format("base::rm(%s)", symbol);
    commands.evaluate(command).get();
  }

  /** Copy (variables of) a table into a symbol. */
  @PostMapping(value = "/symbols/{symbol}")
  public ResponseEntity<Void> loadTable(
      @Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol,
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

  /** Assign the result of the evaluation of an expression to a symbol. */
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

  @PostMapping(value = "/execute", consumes = TEXT_PLAIN_VALUE)
  public CompletableFuture<ResponseEntity<byte[]>> execute(
      @RequestBody String expression, @RequestParam(defaultValue = "false") boolean async) {
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

  /** @return command object (with expression and status) */
  @GetMapping(value = "/lastcommand", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ArmadilloCommandDTO> getLastCommand() {
    return ResponseEntity.of(commands.getLastCommand());
  }

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

  /** Debug an expression */
  @PreAuthorize("hasRole('ROLE_SU')")
  @PostMapping(value = "/debug", consumes = TEXT_PLAIN_VALUE, produces = APPLICATION_JSON_VALUE)
  public Object debug(@RequestBody String expression)
      throws ExecutionException, InterruptedException, REXPMismatchException {
    return commands.evaluate(expression).get().asNativeJavaObject();
  }

  /**
   * @return the available assign {@link org.obiba.datashield.core.impl.PackagedFunctionDSMethod}s
   */
  @GetMapping(value = "/methods/assign", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAssignMethods() {
    return environments.getEnvironment(ASSIGN).getMethods();
  }

  /**
   * @return the available aggregate {@link
   *     org.obiba.datashield.core.impl.PackagedFunctionDSMethod}s
   */
  @GetMapping(value = "/methods/aggregate", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAggregateMethods() {
    return environments.getEnvironment(AGGREGATE).getMethods();
  }

  /** @return the available user {@link Workspace}s */
  @GetMapping(value = "/workspaces", produces = APPLICATION_JSON_VALUE)
  public List<Workspace> getWorkspaces(Principal principal) {
    return commands.listWorkspaces(principal.getName() + "/");
  }

  /**
   * Deletes a workspace (fails silently if the workspace doesn't exist)
   *
   * @param id the id of the saved workspace
   */
  @DeleteMapping(value = "/workspaces/{id}")
  @ResponseStatus(OK)
  public void removeWorkspace(@PathVariable String id, Principal principal) {
    String objectName = format(WORKSPACE_OBJECTNAME_TEMPLATE, principal.getName(), id);
    commands.removeWorkspace(objectName);
  }

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

  @PreAuthorize("hasPermission(#workspace, 'Workspace', 'load')")
  @PostMapping(value = "/load-tables")
  public void loadTables(@RequestParam List<String> workspace)
      throws ExecutionException, InterruptedException {
    commands.loadWorkspaces(workspace.stream().map(it -> it + ".RData").collect(toList())).get();
  }

  @PostMapping(value = "/load-workspace")
  public void loadUserWorkspace(@RequestParam String id, Principal principal)
      throws ExecutionException, InterruptedException {
    String objectName = format(WORKSPACE_OBJECTNAME_TEMPLATE, principal.getName(), id);
    commands.loadUserWorkspace(objectName).get();
  }
}
