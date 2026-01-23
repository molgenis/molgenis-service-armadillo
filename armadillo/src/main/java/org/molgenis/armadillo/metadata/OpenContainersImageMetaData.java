package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public record OpenContainersImageMetaData(
    @Nullable String openContainersId, @Nullable String creationDate) {}
