package org.molgenis.armadillo.controller;

import javax.validation.constraints.NotNull;

public record ObjectRequestBody(@NotNull String name) {}
