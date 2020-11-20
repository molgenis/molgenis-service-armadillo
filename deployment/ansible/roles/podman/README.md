Podman
=========
This installs the Podman (a sort of Kubernetes agent for CentOS / Redhat) on your system. This allows you to run Rserver on the system.

[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-podman-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)

Requirements
------------
The only requirement you need a clean base image of CentOS => 7.

Role Variables
--------------
No variables are needed for this role.

Dependencies
------------
No direct dependencies for this role

Example Playbook
----------------
You can include the podman-role by adding the yaml block below.

    - hosts: all
      roles:
       - role: podman
                   
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org