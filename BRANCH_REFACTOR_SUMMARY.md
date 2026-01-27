Branch refactor/container-classes: summary vs master

Goal
- Decouple container orchestration from DataSHIELD so other container types can be supported.

High-level changes
- Profiles -> containers across API, services, controllers, metadata, configs, tests, docs, and UI.
- Container orchestration generalized via container-agnostic interfaces and schedulers.
- DataSHIELD-specific behavior moved behind explicit DS container types/configs.

Container orchestration generalized (incl. interfaces)
- New container-agnostic lifecycle interfaces: UpdatableContainer, OpenContainer, ContainerUpdater, OpenContainersUpdater.
- New orchestration components: ContainerScheduler, ContainerStatusService, ContainerScope/ContainerScopeConfig.
- Docker orchestration moved into the container layer (DockerService, DockerClientConfig) to work with container metadata/config.
- Generic config/metadata model for containers: ContainerConfig, DefaultContainerConfig, InitialContainerConfig(s), ContainersMetadata/ContainersLoader.
- Interfaces let DS and future container types plug into the same scheduling/update pipeline.

DataSHIELD explicit, not implicit
- DS-only types added: DatashieldContainerConfig, DatashieldContainerFactory, DatashieldContainerUpdater,
  DatashieldInitialConfigBuilder, DatashieldRConnectionFactoryProvider.
- DefaultContainerConfig/DefaultContainerFactory introduced for non-DS container types.
- DS-only behavior guarded by DS-specific config instead of assuming all containers are DS.

API/controller changes
- New container controllers and responses: ContainersController, ContainerDockerController, ContainerResponse,
  ContainersResponse, ContainersStatusResponse, ContainerStartStatusController.
- Container selection/status endpoints updated from profile semantics.

Tests/docs/scripts/configs
- Test suite updated for container naming and behavior; profile tests removed and container tests added.
- Docs, Helm values, docker compose, ops scripts, and release tests updated to container terminology.
- UI/API updated to container naming.

Other branch-specific changes
- HTTP test files removed.
- Comment cleanup commit removed comments added in this branch.
