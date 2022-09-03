package org.molgenis.armadillo.interceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addWebRequestInterceptor(new MDCInterceptor());
  }

  /**
   * Enable matching paths with URL encoded slashes. Needed for the Storage API's object endpoints.
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    UrlPathHelper urlPathHelper = new UrlPathHelper();
    urlPathHelper.setUrlDecode(false);
    configurer.setUrlPathHelper(urlPathHelper);
  }
}
