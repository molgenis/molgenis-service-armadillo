# DataSHIELD service
[![Build Status](https://jenkins.dev.molgenis.org/buildStatus/icon?job=molgenis%2Fmolgenis-service-datashield%2Fmaster)](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-datashield/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Adatashield-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Adatashield-service)

The stack consists of 2 components:
- DataSHIELD service
- RServer

# Development
You can run ```docker-compose up --build```. This builds the jar-file and Dockerfile and spins up the stack.

If you want to run only 1 service:

```docker-compose up rserver```
