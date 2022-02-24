package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;
import static org.molgenis.armadillo.audit.AuditEventPublisher.ASSIGN1;
import static org.molgenis.armadillo.audit.AuditEventPublisher.ASSIGN_FAILURE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.DEBUG;
import static org.molgenis.armadillo.audit.AuditEventPublisher.DELETE_USER_WORKSPACE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.EXECUTE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.EXECUTE_FAILURE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.EXPRESSION;
import static org.molgenis.armadillo.audit.AuditEventPublisher.FOLDER;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_AGGREGATE_METHODS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_ASSIGNED_SYMBOLS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_ASSIGN_METHODS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_PACKAGES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_RESOURCES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_TABLES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.GET_USER_WORKSPACES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.ID;
import static org.molgenis.armadillo.audit.AuditEventPublisher.INSTALL_PACKAGES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LOAD_RESOURCE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LOAD_RESOURCE_FAILURE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LOAD_TABLE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LOAD_TABLE_FAILURE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.LOAD_USER_WORKSPACE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.MESSAGE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROFILES;
import static org.molgenis.armadillo.audit.AuditEventPublisher.PROJECT;
import static org.molgenis.armadillo.audit.AuditEventPublisher.REMOVE_SYMBOL;
import static org.molgenis.armadillo.audit.AuditEventPublisher.RESOURCE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.RESOURCE_EXISTS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.SAVE_USER_WORKSPACE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.SELECTED_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.SELECT_PROFILE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.SYMBOL;
import static org.molgenis.armadillo.audit.AuditEventPublisher.TABLE;
import static org.molgenis.armadillo.audit.AuditEventPublisher.TABLE_EXISTS;
import static org.molgenis.armadillo.audit.AuditEventPublisher.TYPE;
import static org.molgenis.armadillo.controller.ArmadilloUtils.getLastCommandLocation;
import static org.molgenis.armadillo.controller.ArmadilloUtils.serializeExpression;
import static org.obiba.datashield.core.DSMethodType.AGGREGATE;
import static org.obiba.datashield.core.DSMethodType.ASSIGN;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NO_CONTENT;
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
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.exceptions.ExpressionException;
import org.molgenis.armadillo.minio.ArmadilloStorageService;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.armadillo.service.DSEnvironmentCache;
import org.molgenis.armadillo.service.ExpressionRewriter;
import org.molgenis.r.model.RPackage;
import org.obiba.datashield.core.DSMethod;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo", version = "0.1.0"),
    security = {
        @SecurityRequirement(name = "JSESSIONID"),
        @SecurityRequirement(name = "http"),
        @SecurityRequirement(name = "jwt")
    })
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@SecurityScheme(name = "jwt", in = HEADER, type = APIKEY)
@RestController
@Validated
public class DataController {

  public static final String SYMBOL_RE = "\\p{Alnum}[\\w.]*";
  public static final String SYMBOL_CSV_RE = "\\p{Alnum}[\\w.]*+(,\\p{Alnum}[\\w.]*+)*+";
  public static final String WORKSPACE_ID_FORMAT_REGEX = "[\\w-:]+";
  public static final String TABLE_RESOURCE_REGEX =
      "^([a-z0-9-]{0,55}[a-z0-9])/([\\w-:]+)/([\\w-:]+)$";
  public static final String PATH_FORMAT = "%s/%s";

  private final Commands commands;
  private final ArmadilloStorageService storage;
  private final AuditEventPublisher auditEventPublisher;
  private final ExpressionRewriter expressionRewriter;
  private final DSEnvironmentCache dsEnvironmentCache;
  private final java.util.regex.Pattern tableResourcePattern =
      java.util.regex.Pattern.compile(TABLE_RESOURCE_REGEX);

  public DataController(
      Commands commands,
      ArmadilloStorageService storage,
      AuditEventPublisher auditEventPublisher,
      ExpressionRewriter expressionRewriter,
      DSEnvironmentCache dsEnvironmentCache) {
    this.commands = commands;
    this.storage = storage;
    this.auditEventPublisher = auditEventPublisher;
    this.expressionRewriter = expressionRewriter;
    this.dsEnvironmentCache = dsEnvironmentCache;
  }

  @Operation(summary = "Get R packages", description = "Get all installed R packages.")
  @GetMapping(value = "/packages", produces = APPLICATION_JSON_VALUE)
  public List<RPackage> getPackages(Principal principal)
      throws ExecutionException, InterruptedException {
    return auditEventPublisher
        .audit(commands.getPackages(), principal, GET_PACKAGES, Map.of())
        .get();
  }

  @Operation(
      summary = "Get available tables",
      description =
          "Return a list of (fully qualified) table identifiers available for DataSHIELD operations")
  @GetMapping(value = "/tables", produces = APPLICATION_JSON_VALUE)
  public List<String> getTables(Principal principal) {
    return auditEventPublisher.audit(
        () ->
            storage.listProjects().stream()
                .map(storage::listTables)
                .flatMap(List::stream)
                .collect(toList()),
        principal,
        GET_TABLES,
        Map.of());
  }

  @Operation(
      summary = "Check table existence",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The table exists and is available for DataSHIELD operations")
      })
  @RequestMapping(value = "/tables/{project}/{folder}/{table}", method = HEAD)
  public ResponseEntity<Void> tableExists(
      Principal principal,
      @PathVariable String project,
      @PathVariable String folder,
      @PathVariable String table) {
    final boolean result =
        auditEventPublisher.audit(
            () -> storage.tableExists(project, format(PATH_FORMAT, folder, table)),
            principal,
            TABLE_EXISTS,
            Map.of(PROJECT, project, FOLDER, folder, TABLE, table));
    return result ? ok().build() : notFound().build();
  }

  @Operation(
      summary = "Load table",
      description = "Load a table",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "/load-table")
  public CompletableFuture<ResponseEntity<Void>> loadTable(
      Principal principal,
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String symbol,
      @Valid @Pattern(regexp = TABLE_RESOURCE_REGEX) @RequestParam String table,
      @Valid @Pattern(regexp = SYMBOL_CSV_RE) @RequestParam(required = false) String variables,
      @RequestParam(defaultValue = "false") boolean async) {
    var matcher = tableResourcePattern.matcher(table);
    //noinspection ResultOfMethodCallIgnored
    matcher.find();
    var project = matcher.group(1);
    var folder = matcher.group(2);
    var tableName = matcher.group(3);
    Map<String, Object> data =
        Map.of(SYMBOL, symbol, PROJECT, project, FOLDER, folder, TABLE, tableName);
    if (!storage.tableExists(project, String.format(PATH_FORMAT, folder, tableName))) {
      data = new HashMap<>(data);
      data.put(MESSAGE, "Table not found");
      auditEventPublisher.audit(principal, LOAD_TABLE_FAILURE, data);
      return completedFuture(notFound().build());
    }
    var variableList =
        Optional.ofNullable(variables).map(it -> it.split(",")).stream()
            .flatMap(Arrays::stream)
            .map(String::trim)
            .collect(toList());
    var result =
        auditEventPublisher.audit(
            commands.loadTable(symbol, table, variableList), principal, LOAD_TABLE, data);
    return async
        ? completedFuture(created(getLastCommandLocation()).body(null))
        : result
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  @Operation(
      summary = "Get available resources",
      description =
          "Return a list of (fully qualified) resource identifiers available for DataSHIELD operations")
  @GetMapping(value = "/resources", produces = APPLICATION_JSON_VALUE)
  public List<String> getResources(Principal principal) {
    return auditEventPublisher.audit(
        () ->
            storage.listProjects().stream()
                .map(storage::listResources)
                .flatMap(List::stream)
                .collect(toList()),
        principal,
        GET_RESOURCES,
        Map.of());
  }

  @Operation(
      summary = "Check resource existence",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The resource exists and is available for DataSHIELD operations")
      })
  @RequestMapping(value = "/resources/{project}/{folder}/{resource}", method = HEAD)
  public ResponseEntity<Void> resourceExists(
      Principal principal,
      @PathVariable String project,
      @PathVariable String folder,
      @PathVariable String resource) {
    final boolean result =
        auditEventPublisher.audit(
            () -> storage.resourceExists(project, format(PATH_FORMAT, folder, resource)),
            principal,
            RESOURCE_EXISTS,
            Map.of(PROJECT, project, FOLDER, folder, RESOURCE, resource));
    return result ? ok().build() : notFound().build();
  }

  @Operation(
      summary = "Load resource",
      description = "Load a resource",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "/load-resource")
  public CompletableFuture<ResponseEntity<Void>> loadResource(
      Principal principal,
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String symbol,
      @Valid @Pattern(regexp = TABLE_RESOURCE_REGEX) @RequestParam String resource,
      @RequestParam(defaultValue = "false") boolean async) {
    var pattern = java.util.regex.Pattern.compile(TABLE_RESOURCE_REGEX);
    var matcher = pattern.matcher(resource);
    //noinspection ResultOfMethodCallIgnored
    matcher.find();
    var project = matcher.group(1);
    var folder = matcher.group(2);
    var resourceName = matcher.group(3);
    Map<String, Object> data =
        Map.of(SYMBOL, symbol, PROJECT, project, FOLDER, folder, RESOURCE, resourceName);
    if (!storage.resourceExists(project, String.format(PATH_FORMAT, folder, resourceName))) {
      data = new HashMap<>(data);
      data.put(MESSAGE, "Resource not found");
      auditEventPublisher.audit(principal, LOAD_RESOURCE_FAILURE, data);
      return completedFuture(notFound().build());
    }
    var result =
        auditEventPublisher.audit(
            commands.loadResource(symbol, resource), principal, LOAD_RESOURCE, data);
    return async
        ? completedFuture(created(getLastCommandLocation()).body(null))
        : result
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  @Operation(summary = "Get assigned symbols")
  @GetMapping(value = "/symbols", produces = APPLICATION_JSON_VALUE)
  public List<String> getSymbols(Principal principal)
      throws ExecutionException, InterruptedException, REXPMismatchException {
    CompletableFuture<REXP> result =
        auditEventPublisher.audit(
            commands.evaluate("base::ls()"), principal, GET_ASSIGNED_SYMBOLS, Map.of());
    return asList(result.get().asStrings());
  }

  @Operation(
      summary = "Remove symbol",
      description = "Removes a symbol, making the assigned data inaccessible")
  @DeleteMapping(value = "/symbols/{symbol}")
  public void removeSymbol(
      Principal principal, @Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol)
      throws ExecutionException, InterruptedException {
    String command = format("base::rm(%s)", symbol);
    auditEventPublisher
        .audit(commands.evaluate(command), principal, REMOVE_SYMBOL, Map.of(SYMBOL, symbol))
        .get();
  }

  @Operation(
      summary = "Assign symbol",
      description = "Assign the result of the evaluation of an expression to a symbol")
  @PostMapping(value = "/symbols/{symbol}", consumes = TEXT_PLAIN_VALUE)
  public CompletableFuture<ResponseEntity<Void>> assignSymbol(
      Principal principal,
      @Valid @Pattern(regexp = SYMBOL_RE) @PathVariable String symbol,
      @RequestBody String expression,
      @RequestParam(defaultValue = "false") boolean async) {
    Map<String, Object> data = Map.of(SYMBOL, symbol, EXPRESSION, expression);
    try {
      String rewrittenExpression = expressionRewriter.rewriteAssign(expression);
      CompletableFuture<Void> result =
          auditEventPublisher.audit(
              commands.assign(symbol, rewrittenExpression), principal, ASSIGN1, data);
      return async
          ? completedFuture(created(getLastCommandLocation()).body(null))
          : result
              .thenApply(ResponseEntity::ok)
              .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
    } catch (ExpressionException ex) {
      data = new HashMap<>(data);
      data.put(MESSAGE, ex.getMessage());
      data.put(TYPE, ex.getClass().getSimpleName());
      auditEventPublisher.audit(principal, ASSIGN_FAILURE, data);
      throw ex;
    }
  }

  @Operation(summary = "Execute expression")
  @PostMapping(
      value = "/execute",
      consumes = TEXT_PLAIN_VALUE,
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public CompletableFuture<ResponseEntity<byte[]>> execute(
      Principal principal,
      @RequestBody String expression,
      @Parameter(description = "Indicates if the expression should be executed asynchronously")
      @RequestParam(defaultValue = "false")
          boolean async) {
    Map<String, Object> data = Map.of(EXPRESSION, expression);
    try {
      String rewrittenExpression =
          serializeExpression(expressionRewriter.rewriteAggregate(expression));
      CompletableFuture<REXP> result =
          auditEventPublisher.audit(
              commands.evaluate(rewrittenExpression), principal, EXECUTE, data);
      return async
          ? completedFuture(created(getLastCommandLocation()).body(null))
          : result
              .thenApply(ArmadilloUtils::createRawResponse)
              .thenApply(ResponseEntity::ok)
              .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
    } catch (ExpressionException ex) {
      data = new HashMap<>(data);
      data.put(MESSAGE, ex.getMessage());
      data.put(TYPE, ex.getClass().getSimpleName());
      auditEventPublisher.audit(principal, EXECUTE_FAILURE, data);
      throw ex;
    }
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
  public Object debug(Principal principal, @RequestBody String expression)
      throws ExecutionException, InterruptedException, REXPMismatchException {
    auditEventPublisher.audit(principal, DEBUG, Map.of(EXPRESSION, expression));
    return commands.evaluate(expression).get().asNativeJavaObject();
  }

  @PostMapping(value = "select-profile")
  @ResponseStatus(NO_CONTENT)
  public void selectProfile(Principal principal, @RequestBody @NotBlank String profileName) {
    auditEventPublisher.audit(principal, SELECT_PROFILE, Map.of(SELECTED_PROFILE, profileName));
    commands.selectProfile(profileName.trim());
  }

  @PostMapping(value = "install-package")
  @ResponseStatus(NO_CONTENT)
  @PreAuthorize("hasRole('ROLE_SU')")
  public void installPackage(Principal principal, @RequestParam("file") MultipartFile file)
      throws IOException {
    // TODO only development mode

    commands.installPackage(principal, new ByteArrayResource(file.getBytes()), file.getName());
    auditEventPublisher.audit(
        principal, INSTALL_PACKAGES, Map.of(INSTALL_PACKAGES, file.getName()));
  }

  @GetMapping(value = "profiles")
  public @ResponseBody
  ProfilesResponse listProfiles(Principal principal) {
    return auditEventPublisher.audit(
        () -> ProfilesResponse.create(commands.listProfiles(), commands.getActiveProfileName()),
        principal,
        PROFILES,
        Map.of());
  }

  @Operation(summary = "Get available assign methods")
  @GetMapping(value = "/methods/assign", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAssignMethods(Principal principal) {
    return auditEventPublisher.audit(
        () -> dsEnvironmentCache.getEnvironment(ASSIGN).getMethods(),
        principal,
        GET_ASSIGN_METHODS,
        Map.of());
  }

  @Operation(summary = "Get available aggregate methods")
  @GetMapping(value = "/methods/aggregate", produces = APPLICATION_JSON_VALUE)
  public List<DSMethod> getAggregateMethods(Principal principal) {
    return auditEventPublisher.audit(
        () -> dsEnvironmentCache.getEnvironment(AGGREGATE).getMethods(),
        principal,
        GET_AGGREGATE_METHODS,
        Map.of());
  }

  @Operation(summary = "Get user workspaces")
  @GetMapping(value = "/workspaces", produces = APPLICATION_JSON_VALUE)
  public List<Workspace> getWorkspaces(Principal principal) {
    return auditEventPublisher.audit(
        () -> storage.listWorkspaces(principal), principal, GET_USER_WORKSPACES, Map.of());
  }

  @Operation(
      summary = "Delete user workspace",
      responses = {
          @ApiResponse(responseCode = "200", description = "Workspace was removed or did not exist.")
      })
  @DeleteMapping(value = "/workspaces/{id}")
  @ResponseStatus(OK)
  public void removeWorkspace(
      @PathVariable
      @Pattern(
          regexp = WORKSPACE_ID_FORMAT_REGEX,
          message = "Please use only letters, numbers, dashes or underscores")
          String id,
      Principal principal) {
    auditEventPublisher.audit(
        () -> {
          storage.removeWorkspace(principal, id);
          return null;
        },
        principal,
        DELETE_USER_WORKSPACE,
        Map.of(ID, id));
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
    auditEventPublisher
        .audit(
            commands.saveWorkspace(principal, id), principal, SAVE_USER_WORKSPACE, Map.of(ID, id))
        .get();
  }

  @Operation(summary = "Load user workspace")
  @PostMapping(value = "/load-workspace")
  public void loadUserWorkspace(
      @Pattern(
          regexp = WORKSPACE_ID_FORMAT_REGEX,
          message = "Please use only letters, numbers, dashes or underscores")
      @RequestParam
          String id,
      Principal principal)
      throws ExecutionException, InterruptedException {
    auditEventPublisher
        .audit(
            commands.loadWorkspace(principal, id), principal, LOAD_USER_WORKSPACE, Map.of(ID, id))
        .get();
  }
}
