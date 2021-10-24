package org.molgenis.armadillo.config;

import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.config.RServeConfig;
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
  private final RServeConfig rServeConfig;

  public DataShieldConfigProps(RServeConfig rServeConfig) {
    this.rServeConfig = rServeConfig;
  }

  public void setProfiles(List<ProfileConfigProps> profiles) {
    this.profiles = profiles;
  }

  public List<ProfileConfigProps> getProfiles() {
    return profiles;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return DataShieldConfigProps.class.isAssignableFrom(clazz)
        || ProfileConfigProps.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (target instanceof ProfileConfigProps) {
      var environmentName = ((ProfileConfigProps) target).getEnvironment();

      if (rServeConfig.getEnvironments().stream()
          .map(EnvironmentConfigProps::getName)
          .noneMatch(environmentName::equals)) {
        errors.rejectValue(
            "environment",
            "profile.environment.unknown",
            new Object[] {},
            "No RServe environment defined with name '" + environmentName + "'");
      }
    } else {
      DataShieldConfigProps props = (DataShieldConfigProps) target;
      if (props.getProfiles().stream()
          .map(ProfileConfigProps::getName)
          .noneMatch(DEFAULT::equals)) {
        errors.rejectValue(
            "profiles",
            "datashield.profiles.missing-default",
            new Object[] {},
            "Must specify a profile with name " + DEFAULT);
      }
    }
  }
}
