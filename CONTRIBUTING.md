# Contributing to Armadillo

## Running Armadillo from source code

You can run from source code as follows:

1. Install Java and Docker
2. Checkout the source using ```git clone https://github.com/molgenis/molgenis-service-armadillo.git```
3. Optionally copy ```application.template.yml``` to ```application.yml``` to change settings (will be .gitignored)
4. Compile and execute the code using ```./gradlew run```

Note: contact MOLGENIS team if you want to contribute and need a testing OIDC config that you can run against localhost.

# Developing Armadillo

We use gradle to build:
* run using ```./gradlew run```
* run tests using ```./gradlew test```

We use intellij to develop
* To run or debug in intellij, right click on armadillo/src/main/java/org.molgenis.armdadillo/ArmadilloServiceAppliction and choose 'Run/Debug Armadillo...'
* To run using oidc, create a copy of [application.yml](application.template.yml) in root of your project

We have a swagger-ui to quickly see and test available web services at http://localhost:8080/swagger-ui/ 

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
