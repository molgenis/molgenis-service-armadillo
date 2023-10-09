package org.molgenis.armadillo.interceptor;

import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

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

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**").addResourceLocations("classpath:/public/");
  }

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    logger.info("Configuring Tomcat to allow encoded slashes.");
    return factory ->
        factory.addConnectorCustomizers(
            connector ->
                connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue()));
  }
}
