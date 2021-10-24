package org.molgenis.armadillo.profile;

import java.util.concurrent.CompletableFuture;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileScopeTestController {

  private final TaskExecutor taskExecutor;
  private final BeanA a;

  public ProfileScopeTestController(TaskExecutor taskExecutor, BeanA a) {
    this.taskExecutor = taskExecutor;
    this.a = a;
  }

  @PostMapping("/select-profile")
  public void selectProfile(@RequestParam("profile") String profile) {
    ActiveProfileNameAccessor.setActiveProfileName(profile);
  }

  @GetMapping("/get-profile")
  public CompletableFuture<String> getProfile() {
    return CompletableFuture.supplyAsync(a::getProfileName, taskExecutor);
  }
}
