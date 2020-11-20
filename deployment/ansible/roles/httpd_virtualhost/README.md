Apache HTTPD virtualhosts
=========
The virtualhost configuration proxypasses the Armadillo service and Minio file storage through virtualhosts.

[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-httpd_virtualhosts-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)


Requirements
------------
Check the Apache HTTPD requirements

Role Variables
--------------
>note these variables are propagated from the **httpd**-role.

| Variable           | Required | Default | Choices  | Comments                                                                                                           |
|--------------------|----------|---------|----------|--------------------------------------------------------------------------------------------------------------------|
| setup              | yes      | true                       | na       | Should Apache HTTPD need to be installed                                                       |
| ssl                | yes      | false                      | na       | Do certificates need to be installed. The default SHOULD be true                               |
| hostname.armadillo | yes      | armadillo.internal         | na       | Internal domain for Armadillo service so you can test the deployment                           |
| hostname.storage   | yes      | armadillo-storage.internal | na       | Internal domain for storage backend so you can test the deployment                             |
| ports.storage      | yes      | 9000                       | na       | Port on which the Minio file storage is running used in the `proxypass` block in configuration |
| ports.armadillo    | yes      | 8080                       | na       | Port on which the Armadillo service is running used in the `proxypass` block in configuration  |

Dependencies
------------
This is dependant on the following list of roles:
- httpd

Example Playbook
----------------
You can include the httpd_virtualhosts-role by adding the yaml block below.

    - hosts: all
      roles:
       - role: httpd
         vars:
           httpd:
             setup: true
             ssl: true
             hostnames:
               armadillo: armadillo.internal
               storage: armadillo-storage.internal
           ports:
             armadillo: 8080
             storage: 9000         
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org