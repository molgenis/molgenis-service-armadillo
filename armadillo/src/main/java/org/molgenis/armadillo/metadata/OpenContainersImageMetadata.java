package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public record OpenContainersImageMetadata(
    @Nullable String openContainersId, @Nullable String creationDate) {}
