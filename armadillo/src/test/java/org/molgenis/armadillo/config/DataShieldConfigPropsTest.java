package org.molgenis.armadillo.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.DEFAULT;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class DataShieldConfigPropsTest {

  DataShieldConfigProps dataShieldConfigProps;
  @Mock Errors errors;

  @Test
  void validateNoDefaultProfile() {
    dataShieldConfigProps = new DataShieldConfigProps(List.of());
    dataShieldConfigProps.validate(dataShieldConfigProps, errors);

    verify(errors)
        .rejectValue(
            eq("profiles"),
            eq("default.profile.missing"),
            any(),
            eq("Must specify a profile with name " + DEFAULT));
  }
}