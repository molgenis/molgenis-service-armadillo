package org.molgenis.datashield;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.datashield.DataShieldUtils.GLOBAL_ENV;
import static org.molgenis.datashield.DataShieldUtils.TABLE_ENV;
import static org.molgenis.datashield.DataShieldUtils.serializeExpression;
import static org.molgenis.r.Formatter.stringVector;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.molgenis.datashield.pojo.DataShieldCommandDTO;
import org.molgenis.datashield.service.DataShieldExpressionRewriter;
import org.molgenis.datashield.service.StorageService;
import org.molgenis.r.model.RPackage;
import org.molgenis.r.service.PackageService;
import org.molgenis.r.service.RExecutorService;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.core.io.InputStreamResource;
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
  private final RExecutorService rExecutorService;
  private final DataShieldSession datashieldSession;
  private final DataShieldExpressionRewriter expressionRewriter;
  private final PackageService packageService;
  private final IdGenerator idGenerator;
  private final StorageService storageService;

  public DataController(
      RExecutorService rExecutorService,
      DataShieldSession datashieldSession,
      DataShieldExpressionRewriter expressionRewriter,
      PackageService packageService,
      StorageService storageService,
      IdGenerator idGenerator) {
    this.rExecutorService = rExecutorService;
    this.datashieldSession = datashieldSession;
    this.expressionRewriter = expressionRewriter;
    this.packageService = packageService;
    this.storageService = storageService;
    this.idGenerator = idGenerator;
  }

  @GetMapping(value = "/lastresult", produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(OK)
  public CompletableFuture<ResponseEntity<byte[]>> lastResultRaw() {
    return datashieldSession
        .getLastExecution()
        .map(
            execution ->
                execution
                    .thenApply(DataShieldUtils::createRawResponse)
                    .thenApply(ResponseEntity::ok))
        .orElse(completedFuture(notFound().build()));
  }

  @PostMapping(
      value = "/execute",
      consumes = TEXT_PLAIN_VALUE,
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public CompletableFuture<ResponseEntity<byte[]>> executeRaw(
      @RequestBody String expression, @RequestParam(defaultValue = "false") boolean async) {
    String rewrittenExpression = expressionRewriter.rewriteAggregate(expression);
    CompletableFuture<REXP> result =
        datashieldSession.schedule(serializeExpression(rewrittenExpression));
    return async
        ? createdLastCommand()
        : result.thenApply(DataShieldUtils::createRawResponse).thenApply(ResponseEntity::ok);
  }

  private static <R> CompletableFuture<ResponseEntity<R>> createdLastCommand() {
    return completedFuture(
        created(fromCurrentContextPath().replacePath("/lastcommand").build().toUri()).body(null));
  }

  /** @return command object (with expression and status) */
  @GetMapping(value = "/lastcommand", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<DataShieldCommandDTO> getLastCommand() {
    return ResponseEntity.of(datashieldSession.getLastCommand());
  }

  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<RPackage> getPackages() {
    return datashieldSession.execute(packageService::getInstalledPackages);
  }

  /** @return a list of (fully qualified) table identifiers available for DataSHIELD operations. */
  @GetMapping(value = "/tables", produces = APPLICATION_JSON_VALUE)
  public List<String> getTables() throws REXPMismatchException {
    final String command = format("base::local(base::ls(%s))", TABLE_ENV);
    REXP result =
        datashieldSession.execute(connection -> rExecutorService.execute(command, connection));
    return asList(result.asStrings());
  }

  /** @return OK if the the table exists and is available for DataSHIELD operations. */
  @RequestMapping(value = "/tables/{tableId}", method = HEAD)
  public ResponseEntity<Void> tableExists(@PathVariable String tableId)
      throws REXPMismatchException {
    return getTables().contains(tableId) ? ok().build() : notFound().build();
  }

  /** @return a list of assigned symbols */
  @GetMapping(value = "/symbols", produces = APPLICATION_JSON_VALUE)
  public List<String> getSymbols() throws REXPMismatchException {
    REXP result =
        datashieldSession.execute(connection -> rExecutorService.execute("base::ls()", connection));
    return asList(result.asStrings());
  }

  /** Copy (variables of) a table into a symbol. */
  @PostMapping(value = "/symbols/{symbol}")
  public ResponseEntity<Void> loadTable(
      @Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol,
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String table,
      @Valid @Pattern(regexp = SYMBOL_CSV_RE) @RequestParam(required = false) String variables)
      throws REXPMismatchException, ExecutionException, InterruptedException {
    if (!getTables().contains(table)) {
      return notFound().build();
    }
    String expression = table;
    if (variables != null) {
      String[] split = variables.split(",");
      expression = format("%s[,%s]", table, stringVector(split));
    }
    expression = format("base::local(%s, envir = %s)", expression, TABLE_ENV);
    datashieldSession.assign(symbol, expression).get();
    return ok().build();
  }

  /** Assign the result of the evaluation of an expression to a symbol. */
  @PostMapping(value = "/symbols/{symbol}", consumes = TEXT_PLAIN_VALUE)
  public CompletableFuture<ResponseEntity<Void>> assignSymbol(
      @Valid @Pattern(regexp = "\\p{Alnum}[\\w.]*") @PathVariable String symbol,
      @RequestBody String expression,
      @RequestParam(defaultValue = "false") boolean async) {
    String rewrittenExpression = expressionRewriter.rewriteAssign(expression);
    CompletableFuture<Void> result = datashieldSession.assign(symbol, rewrittenExpression);
    return async ? createdLastCommand() : result.thenApply(ResponseEntity::ok);
  }

  /** Debug an expression */
  @PreAuthorize("hasRole('ROLE_SU')")
  @PostMapping(value = "/debug", consumes = TEXT_PLAIN_VALUE, produces = APPLICATION_JSON_VALUE)
  public Object debug(@RequestBody String expression) throws REXPMismatchException {
    REXP result =
        datashieldSession.execute(connection -> rExecutorService.execute(expression, connection));
    return result.asNativeJavaObject();
  }

  /** Removes a symbol, making the assigned data inaccessible */
  @DeleteMapping(value = "/symbols/{symbol}")
  public void removeSymbol(
      @Valid @Pattern(regexp = "\\p{Alnum}[\\w.]*") @PathVariable String symbol) {
    String command = format("base::rm(%s)", symbol);
    datashieldSession.execute(connection -> rExecutorService.execute(command, connection));
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
  public String save() {
    UUID saveId = idGenerator.generateId();
    String objectname = format("%s/.RData", saveId.toString());
    datashieldSession.execute(
        connection -> {
          rExecutorService.saveWorkspace(
              connection, is -> storageService.save(is, objectname, APPLICATION_OCTET_STREAM));
          return null;
        });
    return saveId.toString();
  }

  @PreAuthorize("hasRole('ROLE_' + #folder + '_RESEARCHER')")
  @PostMapping(value = "/load-tables/{folder}/{name}")
  public void loadTables(@PathVariable String folder, @PathVariable String name) {
    String objectName = format("%s/%s.RData", folder, name);
    load(objectName, TABLE_ENV);
  }

  @PostMapping(value = "/load-workspace/{saveId}")
  public void loadUserWorkspace(@PathVariable String saveId) {
    String objectName = format("%s/.RData", UUID.fromString(saveId).toString());
    load(objectName, GLOBAL_ENV);
  }

  private void load(String objectName, String environment) {
    datashieldSession.execute(
        connection -> {
          InputStream inputStream = storageService.load(objectName);
          rExecutorService.loadWorkspace(
              connection, new InputStreamResource(inputStream), environment);
          return null;
        });
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
