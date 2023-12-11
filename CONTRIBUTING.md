# Contributing to Armadillo

## Running Armadillo from source code

You can run from source code as follows:

1. Install Java and Docker
2. Checkout the source using `git clone https://github.com/molgenis/molgenis-service-armadillo.git`
3. Optionally copy `application.template.yml` to `application.yml` to change settings (will be .gitignored)
4. Compile and execute the code using `./gradlew run`

Note: contact MOLGENIS team if you want to contribute and need a testing OIDC config that you can run against localhost.

# Developing Armadillo

We use gradle to build:
* run using `./gradlew run`
* run tests using `./gradlew test`

We use intellij to develop
* To run or debug in intellij, right click on armadillo/src/main/java/org.molgenis.armdadillo/ArmadilloServiceAppliction and choose 'Run/Debug Armadillo...'
* To run using oidc, create a copy of [application.yml](application.template.yml) in root of your project

We have a swagger-ui to quickly see and test available web services at http://localhost:8080/swagger-ui/ 

## Components

We have several components

- [Armadillo](./armadillo/src/) source
- [UI](./ui/README.md) readme
- [R](./r/) java integration source
- [docker builds](./docker/) source
- [scripts](./scripts/README.md) migration readme
  - [install](./scripts/install/README.md) readme
  - [release](./scripts/release/) source
  - [ops](./scripts/ops/README.md) readme

## Releasing

We release through [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) which autoincrement using [Semantic versioning](https://semver.org/).

We use mooltiverse [Nyx](https://mooltiverse.github.io/nyx/guide/user/introduction/how-nyx-works/) for changelog and publishing to github.

Run `./gradlew tasks --group Release` to see all release tasks

Use `./gradlew nyxMake` to see what is build in [build/distributions](./build/distributions/).

### Major, Minor, Fix updates

Each commit with `!` is a major update. So use it wisely. You can also add `BREAKING CHANGE:` in the long commit message format.

### Checking log messages

As [changelog template](./changelog-notes.tpl) uses the commit message it is good to check their quality.

List messages to see usage of conventional commits from the past.

```sh
# How many colon usages
git log --pretty=format:"%s" | cut -d: -f1 | sort | uniq -c | sort -n
```

### Building locally

```sh
./gradlew clean assemble
```

## Continuous integration

- we test on each PR and merges on master
- we build docker compose set for CI testing and demo purposes.
  - [CI testing](./docker/ci/README.md)
  - Demo zip file is a delivery you as artifact
  - Master build have a armadillo-compose.zip for download

### Local CI build

```
./gradlew docker
./docker/bin/prepare.bash ci

cd build/docker/armadillo-compose
# Follow README.md to see Armadillo and R images run in container
docker compose build
docker compose up
```

then run `./release-test.R` against this.

### Local CI test of armadillo-compose

Follow [docker CI README.md](./docker/ci/README.md) to run `release-test.R` using `molgenis/r-cicd` image

## Profile xenon with resourcer whitelisted returns a host.docker.internal error
When developing locally, it might be possible to come across the container error: `Could not resolve host: host.docker.internal`, 
especially when developing on a non-supported operating system when resourcer is whitelisted (such as xenon). 
Sadly, the only way around this error is to edit the JAVA source code of Armadillo to include starting with an extra host.
To enable this feature, you must edit the private method `installImage` of [DockerService.java](https://github.com/molgenis/molgenis-service-armadillo/blob/master/armadillo/src/main/java/org/molgenis/armadillo/profile/DockerService.java) `CreateContainerCmd cmd` from `.withHostConfig(new HostConfig().withPortBindings(portBindings))` to `.withHostConfig(new HostConfig().withPortBindings(portBindings).withExtraHosts("host.docker.internal:host-gateway"))`.

Please note that in order for this change to work, you must use Intellij to run Armadillo or compile the new source code.
Also, if you already have a xenon container build and running, stop and remove that container.

# Developing DataSHIELD packages in Armadillo
As package developer will want to push your new packages into a DataSHIELD profile

* You can start Armadillo with defaults as described above; then use admin/admin as authentication
* to see what profile are available and has been selected:
```
curl -u admin:admin http://localhost:8080/profiles
```
* to change selected profile 'my-profile':
```
curl -X POST http://localhost:8080/select-profile \
  -H 'Content-Type: application/json' \
  -d 'default'
```
* to install-packages in DataSHIELD current using admin user:
```
curl -u admin:admin -v \
-H 'Content-Type: multipart/form-data' \
-F "file=@dsBase_6.3.0.tar.gz" \
-X POST http://localhost:8080/install-package
```
* to update whitelist of your current profile:
```
curl -u admin:admin -X POST http://localhost:8080/whitelist/dsBase
```
* to get whitelist of current profile:
```
curl -u admin:admin http://localhost:8080/whitelist
```
