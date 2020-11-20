RServer
=========


[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-rserver-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)

Requirements
------------
This role requires Podman on the CentOS base image. Then the rserver will run out of the box.

Role Variables
--------------
| Variable                | Required | Default | Choices  | Comments                                          |
|-------------------------|----------|---------|----------|---------------------------------------------------|
| debug                   | yes      | false   | na       | Determines wether the RServer runs in debug-mode. |
| version                 | yes      | 0.0.15  | na       | Version of the RServer service.                   |

Dependencies
------------
This is dependant on the following list of roles:
- podman

Example Playbook
----------------
You can include the rserver-role by adding the yaml block below.

    - hosts: all
      roles:
       - role: rserver
         vars:
           debug: false
           version: 1.8.0
           
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org