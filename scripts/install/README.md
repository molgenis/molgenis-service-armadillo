# Armadillo as a service installation

This is the developer guide for the installation scripts

> NOTE: When moving this directory it has consequences for the documentation.

- [./armadillo-setup.sh](./armadillo-setup.sh) is referenced in the online documentation.
- [./armadillo-check-update.sh](./armadillo-check-update.sh) is WIP [Issue 606](https://github.com/molgenis/molgenis-service-armadillo/issues/606)
- [./conf/application.yml](./conf/application.yml) is used for the installations script but is a WIP as we do have [../../application.template.yml](../../application.template.yml) which should be used instead.
- [./conf/armadillo-nginx.conf](./conf/armadillo-nginx.conf) is an example file

## FAQ for devs

- Running `/etc/cron.weekly/armadillo-check-update` manually does not work
- Running `/usr/share/armadillo/application/armadillo-check-update.sh` does
- By adding the setup command into a file it is easier to (re)run it with `--cleanup`
