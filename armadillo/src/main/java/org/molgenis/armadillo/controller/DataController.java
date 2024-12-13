package org.molgenis.armadillo.controller;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.molgenis.armadillo.controller.ArmadilloUtils.getLastCommandLocation;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.LINK_FILE;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.PARQUET;
import static org.obiba.datashield.core.DSMethodType.AGGREGATE;
import static org.obiba.datashield.core.DSMethodType.ASSIGN;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.InputStream;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.command.ArmadilloCommandDTO;
import org.molgenis.armadillo.command.Commands;
import org.molgenis.armadillo.exceptions.ExpressionException;
import org.molgenis.armadillo.exceptions.UnknownObjectException;
import org.molgenis.armadillo.exceptions.UnknownVariableException;
import org.molgenis.armadillo.model.Workspace;
import org.molgenis.armadillo.service.DSEnvironmentCache;
import org.molgenis.armadillo.service.ExpressionRewriter;
import org.molgenis.armadillo.storage.ArmadilloLinkFile;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.molgenis.r.RServerResult;
import org.molgenis.r.model.RPackage;
import org.obiba.datashield.core.DSMethod;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "DataSHIELD", description = "Core API that interacts with the DataSHIELD environments")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "JSESSIONID")
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

  public DataController(
      Commands commands,
      ArmadilloStorageService storage,
      AuditEventPublisher auditEventPublisher,
      ExpressionRewriter expressionRewriter,
      DSEnvironmentCache dsEnvironmentCache) {
    this.commands = requireNonNull(commands);
    this.storage = requireNonNull(storage);
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
    this.expressionRewriter = requireNonNull(expressionRewriter);
    this.dsEnvironmentCache = requireNonNull(dsEnvironmentCache);
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
            storage.listProjects().stream().map(storage::listTables).flatMap(List::stream).toList(),
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
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Object loaded successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Unknown project or object",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping(value = "/load-table")
  public CompletableFuture<ResponseEntity<Void>> loadTable(
      Principal principal,
      @Valid @Pattern(regexp = SYMBOL_RE) @RequestParam String symbol,
      @Valid @Pattern(regexp = TABLE_RESOURCE_REGEX) @RequestParam String table,
      @Valid @Pattern(regexp = SYMBOL_CSV_RE) @RequestParam(required = false) String variables,
      @RequestParam(defaultValue = "false") boolean async) {
    java.util.regex.Pattern tableResourcePattern =
        java.util.regex.Pattern.compile(TABLE_RESOURCE_REGEX);
    HashMap<String, Object> data = getMatchedData(tableResourcePattern, table, TABLE);
    data.put(SYMBOL, symbol);
    String project = (String) data.get(PROJECT);
    String objectName = String.format(PATH_FORMAT, data.get(FOLDER), data.get(TABLE));
    if (storage.hasObject(project, objectName + LINK_FILE)) {
      return loadTableFromLinkFile(project, objectName, variables, principal, data, symbol, async);
    } else if (storage.hasObject(project, objectName + PARQUET)) {
      var variableList = getVariableList(variables);
      return doLoadTable(symbol, table, variableList, principal, data, async);
    } else {
      data = new HashMap<>(data);
      data.put(MESSAGE, "Table not found");
      auditEventPublisher.audit(principal, LOAD_TABLE_FAILURE, data);
      throw new ResponseStatusException(
          NOT_FOUND, format("Project '%s' has no object '%s'", project, objectName));
    }
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
                .toList(),
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
    Map<String, Object> data = getMatchedData(pattern, resource, RESOURCE);
    data.put(SYMBOL, symbol);
    if (!storage.resourceExists(
        (String) data.get(PROJECT),
        String.format(PATH_FORMAT, data.get(FOLDER), data.get(RESOURCE)))) {
      data = new HashMap<>(data);
      data.put(MESSAGE, "Resource not found");
      auditEventPublisher.audit(principal, LOAD_RESOURCE_FAILURE, data);
      return completedFuture(notFound().build());
    }
    var result =
        auditEventPublisher.audit(
            commands.loadResource(principal, symbol, resource), principal, LOAD_RESOURCE, data);
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
    CompletableFuture<RServerResult> result =
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
      String rewrittenExpression = expressionRewriter.rewriteAggregate(expression);
      CompletableFuture<RServerResult> result =
          auditEventPublisher.audit(
              commands.evaluate(rewrittenExpression, true), principal, EXECUTE, data);
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
    auditEventPublisher.audit(
        () -> commands.selectProfile(profileName.trim()),
        principal,
        SELECT_PROFILE,
        Map.of(SELECTED_PROFILE, profileName));
  }

  @GetMapping(value = "profiles")
  public @ResponseBody ProfilesResponse listProfiles(Principal principal) {
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

  @Operation(summary = "Get all workspaces")
  @GetMapping(value = "/all-workspaces", produces = APPLICATION_JSON_VALUE)
  public Map<String, List<Workspace>> getAllUserWorkspaces(Principal principal) {
    return auditEventPublisher.audit(
        storage::listAllUserWorkspaces, principal, GET_ALL_USER_WORKSPACES, Map.of());
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

  @DeleteMapping(value = "/workspaces/{user}/{id}")
  @ResponseStatus(OK)
  public void removeUserWorkspace(
      @PathVariable
          @Pattern(
              regexp = WORKSPACE_ID_FORMAT_REGEX,
              message = "Please use only letters, numbers, dashes or underscores")
          String user,
      String id,
      Principal principal) {
    auditEventPublisher.audit(
        () -> {
          storage.removeWorkspaceByStringUserId(user, id);
          return null;
        },
        principal,
        DELETE_USER_WORKSPACE,
        Map.of(ID, id, USER, user));
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

  HashMap<String, Object> getMatchedData(
      java.util.regex.Pattern pattern, String value, String resource) {
    var matcher = pattern.matcher(value);
    //noinspection ResultOfMethodCallIgnored
    matcher.find();
    HashMap<String, Object> groups = new HashMap<>();
    groups.put(PROJECT, matcher.group(1));
    groups.put(FOLDER, matcher.group(2));
    groups.put(resource, matcher.group(3));
    return groups;
  }

  public List<String> getVariableList(String variables) {
    return Optional.ofNullable(variables).map(it -> it.split(",")).stream()
        .flatMap(Arrays::stream)
        .map(String::trim)
        .toList();
  }

  private CompletableFuture<ResponseEntity<Void>> doLoadTable(
      String symbol,
      String table,
      List<String> variableList,
      Principal principal,
      Map<String, Object> data,
      Boolean async) {
    var result =
        auditEventPublisher.audit(
            commands.loadTable(symbol, table, variableList), principal, LOAD_TABLE, data);
    return async
        ? completedFuture(created(getLastCommandLocation()).body(null))
        : result
            .thenApply(ResponseEntity::ok)
            .exceptionally(t -> status(INTERNAL_SERVER_ERROR).build());
  }

  protected List<String> getLinkedVariables(ArmadilloLinkFile linkFile, String variables) {
    List<String> allowedVariables = List.of(linkFile.getVariables().split(","));
    List<String> variableList = getVariableList(variables);
    var invalidVariables =
        variableList.stream().filter(element -> !allowedVariables.contains(element)).toList();
    if (invalidVariables.size() > 0) {
      String invalid = invalidVariables.toString();
      throw new UnknownVariableException(linkFile.getProject(), linkFile.getLinkObject(), invalid);
    }
    return variableList.size() == 0
        ? allowedVariables
        : variableList.stream().filter(allowedVariables::contains).toList();
  }

  private CompletableFuture<ResponseEntity<Void>> loadTableFromLinkFile(
      String project,
      String objectName,
      String variables,
      Principal principal,
      HashMap<String, Object> data,
      String symbol,
      Boolean async) {
    InputStream armadilloLinkFileStream = storage.loadObject(project, objectName + LINK_FILE);
    ArmadilloLinkFile linkFile =
        storage.createArmadilloLinkFileFromStream(armadilloLinkFileStream, project, objectName);
    String sourceProject = linkFile.getSourceProject();
    String sourceObject = linkFile.getSourceObject();
    if (runAsSystem(() -> storage.hasObject(sourceProject, sourceObject + PARQUET))) {
      List<String> variableList = getLinkedVariables(linkFile, variables);
      HashMap<String, Object> finalData = data;
      return runAsSystem(
          () ->
              doLoadTable(
                  symbol,
                  sourceProject + "/" + sourceObject,
                  variableList,
                  principal,
                  finalData,
                  async));
    } else {
      data = new HashMap<>(data);
      data.put(MESSAGE, "Object not found");
      auditEventPublisher.audit(principal, LOAD_TABLE_FAILURE, data);
      throw new UnknownObjectException(sourceProject, sourceObject);
    }
  }
}
