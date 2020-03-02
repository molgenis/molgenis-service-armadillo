package org.molgenis.datashield.r;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.molgenis.datashield.service.PackageService;
import org.molgenis.datashield.service.model.Package;
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
  private final RConfigProperties rConfigProperties;
  private final PackageService packageService;
  private Map<String, String> options;

  public DataShieldOptionsImpl(RConfigProperties rConfigProperties, PackageService packageService) {
    this.rConfigProperties = rConfigProperties;
    this.packageService = packageService;
  }

  /*
   * Creates a new connection used to query installed packages.
   * This is a separate method so that it can be mocked in the test.
   * We cannot use the RConnectionFactory cause that would create a circular dependency.
   */
  RConnection createConnection() throws RserveException {
    return new RConnection(rConfigProperties.getHost(), rConfigProperties.getPort());
  }

  @PostConstruct
  public void init() throws RserveException, REXPMismatchException {
    RConnection connection = createConnection();
    options =
        packageService.getInstalledPackages(connection).stream()
            .map(Package::options)
            .filter(Objects::nonNull)
            .collect(HashMap::new, Map::putAll, Map::putAll);
    options.putAll(rConfigProperties.getOptions());
  }

  @Override
  public ImmutableMap<String, String> getValue() {
    return ImmutableMap.copyOf(options);
  }
}
