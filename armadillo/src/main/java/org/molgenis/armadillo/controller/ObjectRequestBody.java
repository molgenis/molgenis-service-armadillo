package org.molgenis.armadillo.controller;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public record ObjectRequestBody(@NotNull @NotEmpty String name) {}
