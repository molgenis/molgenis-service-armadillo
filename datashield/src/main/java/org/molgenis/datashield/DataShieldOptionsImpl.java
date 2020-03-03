package org.molgenis.datashield;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.Package;
import org.molgenis.r.service.PackageService;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
/**
 * Retrieves and combines datashield options. These are defined by:
 *
 * <ol>
 *   <li>The Options fields in the * installed R packages
 *   <li>The rserve.options application properties
 * </ol>
 */
public class DataShieldOptionsImpl implements DataShieldOptions {
  private final DataShieldProperties dataShieldProperties;
  private final PackageService packageService;
  private Map<String, String> options;
  private RConnectionFactory rConnectionFactory;

  public DataShieldOptionsImpl(
      DataShieldProperties dataShieldProperties,
      PackageService packageService,
      RConnectionFactory rConnectionFactory) {
    this.dataShieldProperties = dataShieldProperties;
    this.packageService = packageService;
    this.rConnectionFactory = rConnectionFactory;
  }

  @PostConstruct
  public void init() throws RserveException, REXPMismatchException {
    RConnection connection = rConnectionFactory.retryCreateConnection();
    options =
        packageService.getInstalledPackages(connection).stream()
            .map(Package::options)
            .filter(Objects::nonNull)
            .collect(HashMap::new, Map::putAll, Map::putAll);
    options.putAll(dataShieldProperties.getOptions());
  }

  @Override
  public ImmutableMap<String, String> getValue() {
    return ImmutableMap.copyOf(options);
  }
}
