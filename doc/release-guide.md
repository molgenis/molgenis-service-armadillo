# How to release
## Prerequisites
Make sure the correct SNAPSHOT version is set in the `revision` variable in the root `pom.xml` (this is the version
that will be released, without the `-SNAPSHOT`, of course 😉).

## Actual release
1. Go to [Jenkins](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-armadillo/job/master/).
2. If you have just merged a PR, wait for the last build, else click `Build Now` (left in the menu).
3. Wait for the popup asking to release under the `Prepare Release [ master ]` step in the table.
4. Depending on the changes in the release (breaking change/new feature/bugfix) choose the correct release type
(major/minor/patch).
5. Wait for the release to finish.
6. Check if the new release is published on [dockerhub](https://registry.hub.docker.com/r/molgenis/armadillo/).
7. Finish up the GitHub release notes and publish them.

## During development
The maven release plugin automatically bumps the version with a patch. If you merge any features or breaking changes,
you need to manually bump the `revision` in the root `pom.xml` by a minor or major version respectively.