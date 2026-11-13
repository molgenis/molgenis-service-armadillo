package org.molgenis.armadillo.metadata;

public record ContainerStartStatus(
    String containerName, String status, Integer completedLayers, Integer totalLayers) {}
