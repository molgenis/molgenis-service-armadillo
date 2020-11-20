Apache HTTPD
=========
The virtualhost configuration proxypasses the Armadillo service and Minio file storage through virtualhosts.

[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-apache--httpd-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)

Requirements
------------
The Apache HTTPD needs SSL certificates to work for your domain. It requires 2 subdomains. One for the Aramdillo service and 
one for the storage backend. 

Role Variables
--------------
| Variable             | Required | Default                    | Choices  | Comments                                                             |
|----------------------|----------|----------------------------|----------|----------------------------------------------------------------------|
| enabled              | yes      | true                       | na       | Should Apache HTTPD need to be installed                             |
| ssl.enabled          | yes      | false                      | na       | Do certificates need to be installed. The default SHOULD be true     |
| ssl.paths.server_crt | yes      | /tmp                       | na       | Path to server certificate                                           |
| ssl.paths.server_key | yes      | /tmp                       | na       | Path to private key                                                  |
| ssl.paths.chain_crt  | yes      | /tmp                       | na       | Path to chain certificate                                            |
| hostname.armadillo   | yes      | armadillo.internal         | na       | Internal domain for Armadillo service so you can test the deployment |
| hostname.storage     | yes      | armadillo-storage.internal | na       | Internal domain for storage backend so you can test the deployment   |

Dependencies
------------
This is dependant on the following list of roles:
- minio
- armadillo

Example Playbook
----------------
You can include the httpd-role by adding the yaml block below.

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
               
           
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org