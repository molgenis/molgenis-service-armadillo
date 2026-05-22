package org.molgenis.armadillo.info;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.HashMap;
import java.util.Map;
import org.molgenis.armadillo.security.OidcConfig;
import org.molgenis.armadillo.service.ManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class CustomInfoContributor implements InfoContributor {

  @Autowired ManagementService managementService;

  @Override
  public void contribute(Info.Builder builder) {
    OidcConfig oidcConfig = runAsSystem(managementService::getOidcConfig);
    Map<String, String> authDetails = new HashMap<>();
    authDetails.put("clientId", oidcConfig.clientId());
    authDetails.put("issuerUri", oidcConfig.issuerUri());
    builder.withDetail("auth", authDetails);
  }
}
