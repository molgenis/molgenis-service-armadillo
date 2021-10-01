package org.molgenis.armadillo.profile;

import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.armadillo.service.ArmadilloConnectionFactoryImpl;
import org.molgenis.armadillo.service.DSEnvironmentCache;
import org.molgenis.armadillo.service.ExpressionRewriter;
import org.molgenis.armadillo.service.ExpressionRewriterImpl;
import org.molgenis.r.RConnectionFactory;

/** Everything that used to be a bean but now no longer isn't. */
public class Profile {
  private final ExpressionRewriter expressionRewriter;
  private final ProfileConfigProps profileConfig;
  private final ArmadilloConnectionFactory armadilloConnectionFactory;
  private final DSEnvironmentCache environments;
  private final DataShieldOptions dataShieldOptions;

  public Profile(
      ProfileConfigProps profileConfig,
      RConnectionFactory rConnectionFactory,
      DSEnvironmentCache environments,
      DataShieldOptions dataShieldOptions) {
    this.expressionRewriter = new ExpressionRewriterImpl(environments);
    this.profileConfig = profileConfig;
    this.dataShieldOptions = dataShieldOptions;
    this.armadilloConnectionFactory =
        new ArmadilloConnectionFactoryImpl(dataShieldOptions, rConnectionFactory);
    this.environments = environments;
  }

  public void init() {
    environments.populateEnvironments();
    dataShieldOptions.init();
  }

  public ArmadilloConnectionFactory getArmadilloConnectionFactory() {
    return armadilloConnectionFactory;
  }

  public DSEnvironmentCache getEnvironments() {
    return environments;
  }

  public String getProfileName() {
    return profileConfig.getName();
  }

  public ExpressionRewriter getExpressionRewriter() {
    return expressionRewriter;
  }
}
