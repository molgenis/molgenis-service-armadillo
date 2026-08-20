package org.molgenis.armadillo.controller;

import jakarta.validation.constraints.NotBlank;

public record FabWhitelistEntryRequest(
    @NotBlank String fabId, @NotBlank String fabVersion, @NotBlank String fabHash) {}
