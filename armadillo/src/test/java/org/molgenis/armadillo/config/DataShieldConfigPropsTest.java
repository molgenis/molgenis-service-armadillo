package org.molgenis.armadillo.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.config.RServeConfig;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class DataShieldConfigPropsTest {

  DataShieldConfigProps dataShieldConfigProps;
  @Mock RServeConfig rServeConfig;
  @Mock ProfileConfigProps profileConfigProps;
  @Mock EnvironmentConfigProps environmentConfigProps;
  @Mock Errors errors;

  @BeforeEach
  void beforeEach() {
    dataShieldConfigProps = new DataShieldConfigProps(rServeConfig);
  }

  @Test
  void validateNoDefaultProfile() {
    dataShieldConfigProps.setProfiles(List.of());
    dataShieldConfigProps.validate(dataShieldConfigProps, errors);

    verify(errors)
        .rejectValue(
            eq("profiles"),
            eq("datashield.profiles.missing-default"),
            any(),
            eq("Must specify a profile with name " + DEFAULT));
  }

  @Test
  void validateUndefinedEnvironment() {
    when(profileConfigProps.getEnvironment()).thenReturn("foo");
    when(rServeConfig.getEnvironments()).thenReturn(List.of(environmentConfigProps));
    when(environmentConfigProps.getName()).thenReturn("bar");

    dataShieldConfigProps.validate(profileConfigProps, errors);

    verify(errors)
        .rejectValue(
            eq("environment"),
            eq("profile.environment.unknown"),
            any(),
            eq("No RServe environment defined with name 'foo'"));
  }
}
