/// *
// package org.molgenis.armadillo.container;
//
// import com.fasterxml.jackson.annotation.JsonCreator;
// import com.fasterxml.jackson.annotation.JsonInclude;
// import com.fasterxml.jackson.annotation.JsonProperty;
// import com.google.auto.value.AutoValue;
// import jakarta.annotation.Nullable;
// import java.util.Map;
//
// @AutoValue
// @JsonInclude(JsonInclude.Include.NON_NULL)
// public abstract class FlowerSupernodeContainerConfig extends AbstractFlowerContainerConfig
//    implements ContainerConfig {
//
//  @JsonProperty("superlinkAddress")
//  public abstract String getSuperlinkAddress();
//
//  @JsonProperty("partitionId")
//  public abstract String getPartitionId();
//
//  @JsonCreator
//  public static DefaultContainerConfig create(
//      @JsonProperty("name") String newName,
//      @JsonProperty("image") String newImage,
//      @JsonProperty("host") String newHost,
//      @JsonProperty("port") Integer newPort,
//      @JsonProperty("lastImageId") @Nullable String newLastImageId,
//      @JsonProperty("imageSize") @Nullable Long newImageSize,
//      @JsonProperty("installDate") @Nullable String newInstallDate,
//      @JsonProperty("superlinkAddress") String newSuperlinkAddress,
//      @JsonProperty("partitionId") String newPartitionId,
//      @JsonProperty("numPartitions") String newNumPartitions) {
//
//    return builder()
//        .name(newName)
//        .image(newImage)
//        .host(newHost != null ? newHost : "localhost")
//        .port(newPort)
//        .lastImageId(newLastImageId)
//        .imageSize(newImageSize)
//        .installDate(newInstallDate)
//        .superlinkAddress(newSuperlinkAddress)
//        .partitionId(newPartitionId)
//        .numPartitions(newNumPartitions)
//        .build();
//  }
//
//  public static FlowerSupernodeContainerConfig createDefault() {
//    return builder()
//        .name("default")
//        .host("localhost")
//        .port(6311)
//        .superlinkAddress(null)
//        .partitionId(null)
//        .numPartitions(null)
//        .build();
//  }
//
//  @Override
//  public Map<String, Object> getSpecificContainerConfig() {
//    Map<String, Object> specificData = new java.util.HashMap<>();
//    specificData.put("superlinkAddress", getSuperlinkAddress());
//    specificData.put("partitionID", getPartitionId());
//    specificData.put("numPartitions", getNumberPartitons());
//    return specificData;
//  }
// }
// */
