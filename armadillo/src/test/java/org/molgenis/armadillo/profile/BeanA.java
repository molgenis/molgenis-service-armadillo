package org.molgenis.armadillo.profile;

import org.molgenis.armadillo.config.annotation.ProfileScope;
import org.springframework.stereotype.Component;

@ProfileScope
@Component
public class BeanA {
  private final BeanB beanB;

  public BeanA(BeanB beanB) {
    this.beanB = beanB;
  }

  public String getProfileName() {
    return beanB.getProfileName();
  }
}
