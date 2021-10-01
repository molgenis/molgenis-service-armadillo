package org.molgenis.armadillo.profile;

import org.molgenis.armadillo.DataShieldOptions;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.armadillo.service.ArmadilloConnectionFactory;
import org.molgenis.armadillo.service.ArmadilloConnectionFactoryImpl;
import org.molgenis.armadillo.service.DataShieldProfileEnvironments;
import org.molgenis.armadillo.service.ExpressionRewriter;
import org.molgenis.armadillo.service.ExpressionRewriterImpl;
import org.molgenis.r.RConnectionFactory;

/**
 * Everything that used to be a bean but now no longer isn't.
 */
public class Profile {
  private final ExpressionRewriter expressionRewriter;
  private final ProfileConfigProps profileConfig;
  private final ArmadilloConnectionFactory armadilloConnectionFactory;
  private final DataShieldProfileEnvironments environments;
  private final DataShieldOptions dataShieldOptions;

  public Profile(
      ProfileConfigProps profileConfig,
      RConnectionFactory rConnectionFactory,
      DataShieldProfileEnvironments environments,
      DataShieldOptions dataShieldOptions) {
    this.expressionRewriter = new ExpressionRewriterImpl(environments);
    this.profileConfig = profileConfig;
    this.dataShieldOptions = dataShieldOptions;
    this.armadilloConnectionFactory = new ArmadilloConnectionFactoryImpl(dataShieldOptions, rConnectionFactory);
    this.environments = environments;
  }

  public ArmadilloConnectionFactory getArmadilloConnectionFactory() {
    return armadilloConnectionFactory;
  }

  public DataShieldProfileEnvironments getEnvironments() {
    return environments;
  }

  public String getProfileName() {
    return profileConfig.getNode();
  }

  public ExpressionRewriter getExpressionRewriter() {
    return expressionRewriter;
  }
}
