package org.molgenis.armadillo.controller;

import javax.validation.constraints.NotNull;

public record ProjectRequestBody(@NotNull String name) {}
