package org.molgenis.armadillo.metadata;

import jakarta.validation.constraints.NotBlank;

public record FlowerDataRequest(
    @NotBlank String project, @NotBlank String object, @NotBlank String containerName) {}
