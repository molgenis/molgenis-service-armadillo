package org.molgenis.armadillo.config;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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

  public void setProfiles(List<ProfileConfigProps> profiles) {
    this.profiles = profiles;
  }

  public List<ProfileConfigProps> getProfiles() {
    List<ProfileConfigProps> result = new ArrayList<>(profiles);
    // we will also report what is running from environment NEXT to what has been configured
    // this is only useful if you want to manage your images via the older docker compose way

    return result;
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
