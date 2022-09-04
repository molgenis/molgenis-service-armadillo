package org.molgenis.armadillo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "datashield")
@Component
@Validated
public class DataShieldConfigProps implements Validator {
  @NotEmpty @Valid private List<ProfileConfigProps> profiles;
  ArmadilloMetadataService metadataService;

  public DataShieldConfigProps(ArmadilloMetadataService metadataService) {
    this.metadataService = metadataService;
  }

  public void setProfiles(List<ProfileConfigProps> profiles) {
    this.profiles = profiles;
  }

  public List<ProfileConfigProps> getProfiles() {
    List<ProfileConfigProps> result = new ArrayList<>(profiles);
    // todo bootstrap from settings file

    // silly adapter? or nice isolation?
    return metadataService.profileList().stream()
        .map(
            profileDetails -> {
              ProfileConfigProps props = new ProfileConfigProps();
              props.setName(profileDetails.getName());
              props.setPort(profileDetails.getPort());
              return props;
            })
        .collect(Collectors.toList());
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return DataShieldConfigProps.class.isAssignableFrom(clazz)
        || ProfileConfigProps.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    // nothing to do here, because user can add 'default' image later, right?
  }
}
