package org.molgenis.armadillo.controller;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.COOKIE;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.APIKEY;
import static io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@OpenAPIDefinition(
    info = @Info(title = "MOLGENIS Armadillo - profile manager endpoint", version = "0.1.0"),
    security = {
      @SecurityRequirement(name = "JSESSIONID"),
      @SecurityRequirement(name = "http"),
      @SecurityRequirement(name = "jwt")
    })
@SecurityScheme(name = "JSESSIONID", in = COOKIE, type = APIKEY)
@SecurityScheme(name = "http", in = HEADER, type = HTTP, scheme = "basic")
@SecurityScheme(name = "jwt", in = HEADER, type = APIKEY)
@RestController
@Validated
public class ProfileManagerController {
  public static final String ARMADILLO_PROFILE = "org.molgenis.armadillo.profile";
  // remote control for docker
  private DockerClient dockerClient;
  // audit log
  AuditEventPublisher auditEventPublisher;

  public ProfileManagerController(AuditEventPublisher auditEventPublisher) {
    DefaultDockerClientConfig.Builder config =
        DefaultDockerClientConfig.createDefaultConfigBuilder();
    dockerClient = DockerClientBuilder.getInstance(config).build();
    this.auditEventPublisher = requireNonNull(auditEventPublisher);
  }

  @Operation(
      summary = "list profiles",
      description = "List currently installed profiles.",
      security = {@SecurityRequirement(name = "jwt")})
  @GetMapping(value = "profile-manager", produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  public List<ProfileDefinition> listProfiles(Principal principal) {
    List<ProfileDefinition> result = new ArrayList<>();
    result.addAll(
        dockerClient.listContainersCmd().exec().stream()
            .filter(container -> container.getLabels().containsKey(ARMADILLO_PROFILE))
            .map(
                container -> {
                  ProfileDefinition def = new ProfileDefinition();
                  def.setProfileImage(container.getImage());
                  def.setProfileName(container.getLabels().get(ARMADILLO_PROFILE));
                  return def;
                })
            .collect(Collectors.toList()));
    this.auditEventPublisher.audit(principal, "List armadillo profiles", Map.of());
    return result;
  }

  @Operation(
      summary = "add profile",
      description = "Add a new profile",
      security = {@SecurityRequirement(name = "jwt")})
  @PostMapping(value = "profile-manager", produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  public Map<String, String> addProfile(
      Principal principal,
      @RequestParam String name,
      @RequestParam String image,
      @RequestParam String port)
      throws InterruptedException {
    // load the image
    dockerClient
        .pullImageCmd(image)
        .exec(new PullImageResultCallback())
        .awaitCompletion(5, TimeUnit.MINUTES);

    // start the image
    CreateContainerResponse container =
        dockerClient
            .createContainerCmd(image)
            // nice group name in docker, and add the profile label
            .withLabels(
                Map.of("com.docker.compose.project", "armadillo-profiles", ARMADILLO_PROFILE, name))
            // mapping the port
            .withPortBindings(PortBinding.parse(port + ":6311"))
            // mapping the name
            .withName(name)
            .exec();
    dockerClient.startContainerCmd(container.getId()).exec();
    this.auditEventPublisher.audit(
        principal,
        "Created armadillo profile: ",
        Map.of("name", name, "image", image, "port", port));
    return Map.of("message", "Added profile " + name);
  }

  @Operation(
      summary = "remove profile",
      description = "Remove profile",
      security = {@SecurityRequirement(name = "jwt")})
  @DeleteMapping(value = "profile-manager", produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ROLE_SU')")
  public Map<String, String> deleteProfile(Principal principal, @RequestParam String profileName) {
    Map<String, String> result = new LinkedHashMap<>();
    dockerClient
        .listContainersCmd()
        .exec()
        .forEach(
            container -> {
              // silly that we need to add "/" before
              if (container.getLabels().get(ARMADILLO_PROFILE).equals(profileName)) {
                dockerClient.stopContainerCmd(container.getId()).exec();
                dockerClient.removeContainerCmd(container.getId()).exec();
                this.auditEventPublisher.audit(
                    principal, "Deleted profile", Map.of("profileName", profileName));
                result.put("message", "Deleted armadillo profile: " + profileName);
              }
            });
    if (result.isEmpty()) {
      result.put("message", "Delete armadillo profile skipped: " + profileName + " did not exist");
    }
    return result;
  }
}
