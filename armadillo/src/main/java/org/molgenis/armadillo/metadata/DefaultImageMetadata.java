package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public record DefaultImageMetadata(
    @Nullable String currentImageId, @Nullable Long imageSize, @Nullable String installDate) {}
