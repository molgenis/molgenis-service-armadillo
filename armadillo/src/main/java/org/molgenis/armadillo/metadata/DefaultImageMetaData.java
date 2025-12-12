package org.molgenis.armadillo.metadata;

import jakarta.annotation.Nullable;

public record DefaultImageMetaData(
    String currentImageId, Long imageSize, @Nullable String installDate) {}
