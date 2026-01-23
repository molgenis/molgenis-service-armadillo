Branch review checklist: refactor/container-classes vs master

Status legend: [ ] pending, [x] reviewed

[x] README.md
[x] application.template.yml
[x] armadillo/build.gradle
[x] armadillo/http/dummy.parquet
[x] armadillo/http/http-client.env.json
[x] armadillo/http/invalid_rpackage.txt
[x] armadillo/http/test-actuator.http
[x] armadillo/http/test-exceptions.http
[x] armadillo/http/test-profiles.http
[x] armadillo/http/test-resources.http
[x] armadillo/http/test-storage.http
[x] armadillo/http/test-tutorial.http
[x] armadillo/http/test-workspaces.http
[x] armadillo/http/test.http
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
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/DefaultImageMetaData.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialContainerConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialContainerConfigs.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialProfileConfig.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/InitialProfileConfigs.java
[x] armadillo/src/main/java/org/molgenis/armadillo/metadata/OpenContainersImageMetaData.java
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
[x] armadillo/src/test/.DS_Store
[x] armadillo/src/test/java/.DS_Store
[x] armadillo/src/test/java/org/.DS_Store
[x] armadillo/src/test/java/org/molgenis/.DS_Store
[x] armadillo/src/test/java/org/molgenis/armadillo/.DS_Store
[ ] armadillo/src/test/java/org/molgenis/armadillo/DataShieldOptionsImplTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/TestSecurityConfig.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/command/impl/CommandsImplTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/container/ContainerStatusServiceTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/container/DockerServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/container/profileSchedulerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/ContainersControllerDockServiceNullTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/ContainersControllerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/DataControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/DatashieldProfileControllerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/DevelopmentControllerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/InsightControllerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/controller/ProfileStartStatusControllerTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/controller/ProfilesControllerTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/info/RProcessEndpointTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/metadata/ContainerServiceTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/metadata/ContainerStartStatusTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/metadata/DatashieldContainerConfigTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/metadata/DummyContainersLoader.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/DummyProfilesLoader.java
[x] armadillo/src/test/java/org/molgenis/armadillo/metadata/ProfileServiceTest.java
[x] armadillo/src/test/java/org/molgenis/armadillo/profile/DockerServiceTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/security/JwtRolesExtractorTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/service/ArmadilloConnectionFactoryImplTest.java
[ ] armadillo/src/test/java/org/molgenis/armadillo/service/DSEnvironmentConfigPropsCacheTest.java
[ ] armadillo/src/test/resources/load_test.jmx
[ ] armadillo/src/test/resources/profiles.csv
[ ] build.gradle
[ ] ci/ci.env
[ ] docker-compose.yml
[ ] docker/ci/README.md
[ ] docker/ci/application.yml
[ ] docker/ci/armadillo-compose.md
[ ] docker/quickstart/config/application.yml
[ ] docker/quickstart/data/system/profiles.json
[x] docker/quickstart/keycloak/plugin/keycloak-event-listener.jar
[ ] docs/pages/basic_usage/armadillo_ui.md
[ ] docs/pages/dev_guides.md
[ ] docs/pages/faq.md
[ ] docs/pages/install_management/armadillo_management.md
[ ] docs/pages/install_management/armadillo_migrate_2_to_3.md
[ ] docs/pages/install_management/armadillo_migrate_3_to_4.md
[ ] docs/pages/install_management/armadillo_migrate_4_to_5.md
[ ] docs/pages/install_management/armadillo_minor_release_update.md
[ ] docs/pages/quick_start.md
[ ] helm-chart/values.yaml
[ ] scripts/install/conf/application.yml
[ ] scripts/ops/armadilloctl.bash
[ ] scripts/ops/env.dist
[ ] scripts/ops/test_armadilloctl.bash
[ ] scripts/release/lib/release-test-info.R
[ ] scripts/release/release-test.R
[ ] scripts/release/release.Rproj
[ ] scripts/release/test-cases/donkey-tidyverse 2.R
[ ] scripts/release/test-cases/researcher-login.R
[ ] scripts/release/test-cases/setup-containers.R
[x] scripts/release/test-cases/setup-profiles.R
[ ] scripts/release/test-cases/test-config.R
[x] scripts/release/test-cases/verify-container.R
[ ] scripts/release/test-cases/verify-resources.R
[ ] scripts/release/test-cases/xenon-exposome.R
[ ] scripts/release/test-cases/xenon-omics.R
[ ] scripts/rock/rockctl/renv/activate.R
[ ] ui/src/api/api.ts
[ ] ui/src/views/Profiles.vue
[ ] ui/tests/unit/views/Profiles.spec.ts
