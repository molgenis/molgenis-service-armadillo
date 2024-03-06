package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.FileInputStream;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.metadata.FileDetails;
import org.molgenis.armadillo.metadata.FileInfo;
import org.molgenis.armadillo.metadata.InsightService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "insight", description = "Insight API to check Armadillo status")
@RestController
@SecurityRequirement(name = "http")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "JSESSIONID")
@RequestMapping("insight")
@PreAuthorize("hasRole('ROLE_SU')")
public class InsightController {
  private final InsightService insightService;
  private final AuditEventPublisher auditor;

  public InsightController(InsightService insightService, AuditEventPublisher auditor) {
    this.insightService = insightService;
    this.auditor = auditor;
  }

  @Operation(summary = "List files")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "All files",
            content =
                @Content(array = @ArraySchema(schema = @Schema(implementation = FileInfo.class)))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "files", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public List<FileInfo> filesList(Principal principal) {
    return auditor.audit(insightService::filesInfo, principal, LIST_FILES, Map.of());
  }

  @Operation(summary = "File details")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "File details",
            content = @Content(schema = @Schema(implementation = FileDetails.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(hidden = true)))
      })
  @GetMapping(path = "files/{file_id}", produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(OK)
  public FileDetails fileDetails(
      Principal principal,
      @PathVariable String file_id,
      @RequestParam(name = "page_num", required = false, defaultValue = "0") int pageNum,
      @RequestParam(name = "page_size", required = false, defaultValue = "1000") int pageSize,
      @RequestParam(name = "direction", required = false, defaultValue = "end") String direction) {
    return auditor.audit(
        () -> insightService.fileDetails(file_id, pageNum, pageSize, direction),
        principal,
        FILE_DETAILS,
        Map.of("FILE_ID", file_id));
  }

  @Operation(summary = "Download file details")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Download file details"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  @GetMapping(path = "files/{file_id}/download", produces = APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(OK)
  public ResponseEntity<Resource> downloadDetails(
      Principal principal, @PathVariable String file_id) {
    return auditor.audit(
        () -> createDownloadFile(file_id), principal, DOWNLOAD_FILE, Map.of("FILE_ID", file_id));
  }

  public ResponseEntity<Resource> createDownloadFile(String file_id) {
    FileInputStream file = insightService.downloadFile(file_id);
    InputStreamResource inputStreamResource = new InputStreamResource(file);

    HttpHeaders headers = new HttpHeaders();
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + insightService.getFileName(file_id) + "\"");
    headers.add(HttpHeaders.CONTENT_TYPE, "text/text");
    return new ResponseEntity<>(inputStreamResource, headers, OK);
  }
}
