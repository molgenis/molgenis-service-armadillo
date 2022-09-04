package org.molgenis.armadillo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.metadata.ArmadilloMetadataService;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class DataShieldConfigPropsTest {

  DataShieldConfigProps dataShieldConfigProps;

  @Mock Errors errors;
  @Mock ArmadilloMetadataService armadilloMetadataService;

  @BeforeEach
  void beforeEach() {
    dataShieldConfigProps = new DataShieldConfigProps(armadilloMetadataService);
  }

  @Test
  void validateNoDefaultProfile() {}
}
