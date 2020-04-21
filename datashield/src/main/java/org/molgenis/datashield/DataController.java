package org.molgenis.datashield;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.datashield.DataShieldUtils.GLOBAL_ENV;
import static org.molgenis.datashield.DataShieldUtils.TABLE_ENV;
import static org.molgenis.datashield.DataShieldUtils.getLastCommandLocation;
import static org.molgenis.datashield.DataShieldUtils.serializeExpression;
import static org.molgenis.r.Formatter.stringVector;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.molgenis.datashield.command.Commands;
import org.molgenis.datashield.command.DataShieldCommandDTO;
import org.molgenis.datashield.service.DataShieldExpressionRewriter;
import org.molgenis.r.model.RPackage;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.IdGenerator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class DataController {
  public static final String SYMBOL_RE = "\\p{Alnum}[\\w.]*";
  public static final String SYMBOL_CSV_RE = "\\p{Alnum}[\\w.]*(,\\p{Alnum}[\\w.]*)*";
  private final DataShieldExpressionRewriter expressionRewriter;
  private final IdGenerator idGenerator;
  private final Commands commands;

  public DataController(
      DataShieldExpressionRewriter expressionRewriter, IdGenerator idGenerator, Commands commands) {
    this.expressionRewriter = expressionRewriter;
    this.idGenerator = idGenerator;
    this.commands = commands;
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
  public void removeSymbol(
      @Valid @Pattern(regexp = "\\p{Alnum}[\\w.]*") @PathVariable String symbol)
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
      @Valid @Pattern(regexp = "\\p{Alnum}[\\w.]*") @PathVariable String symbol,
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
            .thenApply(DataShieldUtils::createRawResponse)
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  /** @return command object (with expression and status) */
  @GetMapping(value = "/lastcommand", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<DataShieldCommandDTO> getLastCommand() {
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
                    .thenApply(DataShieldUtils::createRawResponse)
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
   * @return a list of available methods (with name, type ('aggregate' or 'assign'), class
   *     ('function' or 'script'), value, package, version.
   */
  @GetMapping(value = "/methods", produces = APPLICATION_JSON_VALUE)
  public List<String> getMethods() {
    // TODO implement
    return Collections.emptyList();
  }

  /** @return a list of workspaces (with lastAccessDate and size) */
  @GetMapping(value = "/workspaces", produces = APPLICATION_JSON_VALUE)
  public List<String> getWorkspaces() {
    // TODO implement
    return Collections.emptyList();
  }

  /**
   * Deletes a workspace (fails silently if the workspace doesn't exist)
   *
   * @param id the id of the saved workspace
   */
  @DeleteMapping(value = "/workspaces/{id}")
  @ResponseStatus(OK)
  public void removeWorkspace(@PathVariable String id) {
    // TODO implement
  }

  @PostMapping(value = "/save-workspace", produces = TEXT_PLAIN_VALUE)
  public String save() throws ExecutionException, InterruptedException {
    UUID saveId = idGenerator.generateId();
    commands.saveWorkspace(format("%s/.RData", saveId.toString())).get();
    return saveId.toString();
  }

  @PreAuthorize("hasRole('ROLE_' + #folder + '_RESEARCHER')")
  @PostMapping(value = "/load-tables/{folder}/{name}")
  public void loadTables(@PathVariable String folder, @PathVariable String name)
      throws ExecutionException, InterruptedException {
    String objectName = format("%s/%s.RData", folder, name);
    commands.loadWorkspace(objectName, TABLE_ENV).get();
  }

  @PostMapping(value = "/load-workspace/{saveId}")
  public void loadUserWorkspace(@PathVariable String saveId)
      throws ExecutionException, InterruptedException {
    String objectName = format("%s/.RData", UUID.fromString(saveId).toString());
    commands.loadWorkspace(objectName, GLOBAL_ENV).get();
  }

  /**
   * @return info object (with repo.version and repo.name, the db.name, username, host, port, etc.)
   */
  @GetMapping(value = "/info", produces = APPLICATION_JSON_VALUE)
  public String getInfo() {
    // TODO implement
    return null;
  }
}
