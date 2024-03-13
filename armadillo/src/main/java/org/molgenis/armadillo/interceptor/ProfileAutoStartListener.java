package org.molgenis.armadillo.interceptor;

import java.util.Map;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.armadillo.profile.ContainerInfo;
import org.molgenis.armadillo.profile.DockerService;
import org.molgenis.armadillo.security.RunAs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ProfileAutoStartListener implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired private DockerService dockerService;

  @Autowired private ProfileService profileService;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    // FIXME: cannot call from here (security context not set)
    // dockerService.doAutoStart();
    long delay = 5 * 1000;
    Map<String, ContainerInfo> allProfiles =
        RunAs.runAsSystem(() -> dockerService.getAllProfileStatuses());
    for (Map.Entry<String, ContainerInfo> entry : allProfiles.entrySet()) {
      String key = entry.getKey();
      ContainerInfo value = entry.getValue();

      // You can now use the key and value.
      System.out.println("Auto starting :" + " Key: " + key + ", Value: " + value);
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        System.out.println("SLEEP ".repeat(10) + "exception");
      }
      RunAs.runAsSystem(() -> dockerService.startProfile(key));
    }
  }
}
