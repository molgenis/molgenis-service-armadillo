package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public record AutoUpdateSchedule(String frequency, @Nullable String day, @Nullable String time) {}
