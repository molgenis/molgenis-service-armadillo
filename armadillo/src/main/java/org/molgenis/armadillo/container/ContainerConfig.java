package org.molgenis.armadillo.container;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DatashieldContainerConfig.class, name = "ds"),
  @JsonSubTypes.Type(value = VanillaContainerConfig.class, name = "vanilla"),
  @JsonSubTypes.Type(value = FlowerSupernodeContainerConfig.class, name = "flower-supernode"),
  @JsonSubTypes.Type(value = FlowerSuperexecContainerConfig.class, name = "flower-superexec")
})
public interface ContainerConfig {

  String getName();

  String getImage();

  @Nullable
  String getHost();

  @Nullable
  Integer getPort();

  @Nullable
  Long getImageSize();

  @Nullable
  String getInstallDate();

  @Nullable
  String getLastImageId();

  @Nullable
  List<String> getDockerArgs();

  @Nullable
  Map<String, Object> getDockerOptions();

  String getType();
}
