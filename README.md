# Armadillo service
[![Build Status](https://jenkins.dev.molgenis.org/buildStatus/icon?job=molgenis%2Fmolgenis-service-armadillo%2Fmaster)](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-armadillo/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

The stack consists of multiple components:
- Armadillo service
- RServer
- MinIO server

# Development
You can run ```docker-compose up --build```. This builds the jar-file and Dockerfile and spins up the stack.

If you want to run only one service:

```docker-compose up rserver```

# Deployment
There are 2 ways to deploy the Armadillo.
- Using [helm](https://helm.io) and [kubernetes](https://kubernets.io)
- Using [ansible](https://ansible.org)

## Helm and kubernetes
To use our helm charts you need to add our helm repository. From there you can install the Armadillo using Helm.

```
helm repo add molgenis https://helm.molgenis.org
```

Install the chart and update the `values.yaml` where needed.

```
helm install molgenis-armadillo
```

## Ansible
For production usage you choose your on CentOS base image as long as it is higher than version 7.x.
When you are using Ansible you can run the playbook this way.

```
ansible-playbook playbook.yml
```


