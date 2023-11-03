package org.molgenis.armadillo.controller;

import jakarta.validation.constraints.NotNull;

public record ProjectRequestBody(@NotNull String name) {}
