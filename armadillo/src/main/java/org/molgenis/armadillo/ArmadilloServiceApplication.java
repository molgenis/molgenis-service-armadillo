package org.molgenis.armadillo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
public class ArmadilloServiceApplication implements WebMvcConfigurer {

  public static void main(String[] args) {
    // Allow URL encoded slashes in Tomcat
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

    SpringApplication.run(ArmadilloServiceApplication.class, args);
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
