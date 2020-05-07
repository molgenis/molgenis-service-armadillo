# Armadillo service
[![Build Status](https://jenkins.dev.molgenis.org/buildStatus/icon?job=molgenis%2Fmolgenis-service-armadillo%2Fmaster)](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-armadillo/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Adatashield-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Adatashield-service)

The stack consists of multiple components:
- Armadillo service
- RServer
- MinIO server

# Development
You can run ```docker-compose up --build```. This builds the jar-file and Dockerfile and spins up the stack.

If you want to run only one service:

```docker-compose up rserver```
