package org.molgenis.armadillo.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ObjectRequestBody(@NotNull @NotEmpty String name) {}
