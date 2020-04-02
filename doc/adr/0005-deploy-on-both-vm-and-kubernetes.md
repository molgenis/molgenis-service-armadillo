# 5. Deploy both on a virtual machine and Kubernetes

Date: 2020-04-01

## Status

Accepted

## Context

Because of the diversity in landscape we need to be able to deploy on different environments

## Decision

We are going to create deployments for virtual machines based on CentOS (>=8) using Ansible and Vagrant. 
Besides that we are going to create a chart which allows you to deploy on Kubernetes  

## Consequences

- Some of the VM environments are not serviced (Ubuntu, Suse, other operating systems)
- Some other container orchestrators are not met as well eg docker-swarm
