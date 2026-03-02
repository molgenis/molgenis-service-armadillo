package org.molgenis.armadillo.controller;

import jakarta.validation.constraints.NotBlank;

public record PushDataRequest(
    @NotBlank String project, @NotBlank String resource, @NotBlank String containerName) {}
