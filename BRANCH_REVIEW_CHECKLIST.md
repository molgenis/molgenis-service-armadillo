Branch review checklist: refactor/container-classes vs master

Status legend: [ ] pending, [x] reviewed

[x] README.md
[x] application.template.yml
[x] armadillo/build.gradle
[x] removed: armadillo/http/dummy.parquet
[x] removed: armadillo/http/http-client.env.json
[x] removed: armadillo/http/invalid_rpackage.txt
[x] removed: armadillo/http/test-actuator.http
[x] removed: armadillo/http/test-exceptions.http
[x] removed: armadillo/http/test-profiles.http
[x] removed: armadillo/http/test-resources.http
[x] removed: armadillo/http/test-storage.http
[x] removed: armadillo/http/test-tutorial.http
[x] removed: armadillo/http/test-workspaces.http
[x] removed: armadillo/http/test.http
[x] armadillo/src/main/java/org/molgenis/armadillo/DataShieldOptionsImpl.java
[x] armadillo/src/main/java/org/molgenis/armadillo/README.md
[x] armadillo/src/main/java/org/molgenis/armadillo/audit/AuditEventPublisher.java
[x] armadillo/src/main/java/org/molgenis/armadillo/command/Commands.java
[x] armadillo/src/main/java/org/molgenis/armadillo/command/CommandsConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/command/impl/CommandsImpl.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ActiveContainerNameAccessor.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerInfo.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerScheduler.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerSchedulerFallbackConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerScope.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerScopeConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerStatusService.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/ContainerUpdater.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DatashieldContainerConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DatashieldContainerFactory.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DatashieldContainerUpdater.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DatashieldInitialConfigBuilder.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DatashieldRConnectionFactoryProvider.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DefaultContainerConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DefaultContainerFactory.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DefaultContainerUpdater.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DockerClientConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DockerService.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/InitialConfigBuilder.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/OpenContainer.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/OpenContainersUpdater.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/UpdatableContainer.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/annotation/ContainerScope.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainerDockerController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainerResponse.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainerStartStatusController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainersController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainersResponse.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ContainersStatusResponse.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/DataController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/DevelopmentController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/InsightController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ProfileResponse.java
[x] armadillo/src/main/java/org/molgenis/armadillo/controller/ProfileStartStatusController.java
[x] armadillo/src/main/java/org/molgenis/armadillo/exceptions/DefaultContainerDeleteException.java
[x] armadillo/src/main/java/org/molgenis/armadillo/exceptions/MissingImageException.java
[x] armadillo/src/main/java/org/molgenis/armadillo/exceptions/UnknownContainerException.java
[x] armadillo/src/main/java/org/molgenis/armadillo/info/RMetrics.java
[x] armadillo/src/main/java/org/molgenis/armadillo/info/RProcessEndpoint.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ContainerService.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ContainerStartStatus.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ContainerStatus.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ContainersLoader.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ContainersMetadata.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/DefaultImageMetadata.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialContainerConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialContainerConfigs.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialProfileConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialProfileConfigs.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/OpenContainersImageMetadata.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ProfileConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ProfileService.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ProfilesLoader.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/ProfilesMetadata.java
[x] armadillo/src/main/java/org/molgenis/armadillo/profile/ProfileScheduler.java
[x] armadillo/src/main/java/org/molgenis/armadillo/profile/ProfileSchedulerFallbackConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/profile/ProfileScopeConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/profile/ProfileStatusService.java
[x] armadillo/src/main/java/org/molgenis/armadillo/security/AuthConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/security/JwtDecoderConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/service/ArmadilloConnectionFactoryImpl.java
[x] armadillo/src/main/java/org/molgenis/armadillo/service/DSEnvironmentCache.java
[x] armadillo/src/main/java/org/molgenis/armadillo/service/ExpressionRewriterImpl.java
[x] armadillo/src/main/java/org/molgenis/armadillo/service/FileService.java
[x] armadillo/src/main/resources/application.yml
[x] armadillo/src/test/java/org/molgenis/armadillo/DataShieldOptionsImplTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/TestSecurityConfig.java
[x] armadillo/src/test/java/org/molgenis/armadillo/command/impl/CommandsImplTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/container/ContainerStatusServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/container/DockerServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/container/ContainerSchedulerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/ContainersControllerDockServiceNullTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/ContainersControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/DataControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/DatashieldProfileControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/DevelopmentControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/InsightControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/ContainerStartStatusControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/ProfilesControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/info/RProcessEndpointTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/ContainerServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/ContainerStartStatusTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/DatashieldContainerConfigTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/DummyContainersLoader.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/DummyProfilesLoader.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/ProfileServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/profile/DockerServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/security/JwtRolesExtractorTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/service/ArmadilloConnectionFactoryImplTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/service/DSEnvironmentConfigPropsCacheTest.java
[x] armadillo/src/test/resources/load_test.jmx
[x] armadillo/src/test/resources/profiles.csv
[x] build.gradle
[x] ci/ci.env
[x] docker-compose.yml
[x] docker/ci/README.md
[x] docker/ci/application.yml
[x] docker/ci/armadillo-compose.md
[x] docker/quickstart/config/application.yml
[x] docker/quickstart/data/system/profiles.json
[x] docker/quickstart/keycloak/plugin/keycloak-event-listener.jar
[x] docs/pages/basic_usage/armadillo_ui.md
[x] docs/pages/dev_guides.md
[x] docs/pages/faq.md
[x] docs/pages/install_management/armadillo_management.md
[x] docs/pages/install_management/armadillo_migrate_2_to_3.md
[x] docs/pages/install_management/armadillo_migrate_3_to_4.md
[x] docs/pages/install_management/armadillo_migrate_4_to_5.md
[x] docs/pages/install_management/armadillo_minor_release_update.md
[x] docs/pages/quick_start.md
[x] helm-chart/values.yaml
[x] scripts/install/conf/application.yml
[x] scripts/ops/armadilloctl.bash
[x] scripts/ops/env.dist
[x] scripts/ops/test_armadilloctl.bash
[x] scripts/release/lib/release-test-info.R
[x] scripts/release/release-test.R
[x] scripts/release/release.Rproj
[x] scripts/release/test-cases/donkey-tidyverse 2.R
[x] scripts/release/test-cases/researcher-login.R
[x] scripts/release/test-cases/setup-containers.R
[x] scripts/release/test-cases/setup-profiles.R
[x] scripts/release/test-cases/test-config.R
[x] scripts/release/test-cases/verify-container.R
[x] scripts/release/test-cases/verify-resources.R
[x] scripts/release/test-cases/xenon-exposome.R
[x] scripts/release/test-cases/xenon-omics.R
[x] scripts/rock/rockctl/renv/activate.R
[x] ui/src/api/api.ts
[x] ui/src/views/Profiles.vue
[x] ui/tests/unit/views/Profiles.spec.ts
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DefaultContainerFactoryConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/container/DefaultContainerFactoryImpl.java
[x] armadillo/src/main/java/org/molgenis/armadillo/exceptions/UnsupportedContainerTypeException.java
[x] removed: armadillo/src/main/java/org/molgenis/armadillo/metadata/ProfileStartStatus.java
[x] removed: armadillo/src/test/java/org/molgenis/armadillo/metadata/ProfileStartStatusTest.java
[x] removed: armadillo/src/test/java/org/molgenis/armadillo/profile/ProfileStatusServiceTest.java
[x] scripts/release/dev.env.dist
