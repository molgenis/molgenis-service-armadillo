package org.molgenis.armadillo.controller;

import static org.molgenis.armadillo.controller.DataController.SYMBOL_CSV_RE;
import static org.molgenis.armadillo.controller.DataController.TABLE_RESOURCE_REGEX;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.RequestParam;

public record LinkedObjectRequestBody(
    @Valid @Pattern(regexp = TABLE_RESOURCE_REGEX) @RequestParam(required = true)
        String linkedObjectName,
    @NotNull @NotEmpty String linkedObjectProject,
    @Valid @Pattern(regexp = SYMBOL_CSV_RE) @RequestParam(required = false) String variables) {}
