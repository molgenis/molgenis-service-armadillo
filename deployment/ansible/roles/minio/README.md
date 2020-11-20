Minio
=========
This installs the Minio file storage on your system. This allows you to store data on the federation service.

[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-minio-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)

Requirements
------------
The only requirement you need a clean base image of CentOS => 7.

Role Variables
--------------
| Variable              | Required | Default                           | Choices  | Comments                                                                                          |
|-----------------------|----------|-----------------------------------|----------|---------------------------------------------------------------------------------------------------| 
| version               | yes      | 2020-11-12T22-33-34Z              | na       | Version of the Minio service. There are monthly releases so you need to upgrade regularly         |
| access_key            | yes      | xxxxxx-xxxxxxx-xxxxxxx            | na       | The access key to access Minio API and webinterface                                               |
| secret_key            | yes      | xxxxxx-xxxxxxx-xxxxxxx            | na       | The secret key to access Minio API and webinterface                                               |
| data                  | yes      | /var/lib/minio/data               | na       | The path on the host system where the of the Minio file storage is stored                         |
| oauth.issuer_uri      | yes      | https://auth.example.org          | na       | The plain url of the authentication server (can be FusionAuth or Keycloack for example            |
| oauth.discovery_path  | yes      | /.well-known/openid-configuration | na       | Discovery path to extract information like the endpoints and other relevant details of the server |
| oauth.client_id       | yes      | xxxxx.xxxxxxx.xxxxxxx             | na       | The client ID of the authentication server                                                        |

Dependencies
------------

This is dependant on the following list of roles:
- minio
- httpd
- httpd_virtualhost

Example Playbook
----------------
You can include the minio-role by adding the yaml block below.

    - hosts: all
      vars:
        oauth: 
          issuer_uri: https://auth.example.org
          discovery_path: /.well-known/openid-configuration
          client_id: xxxxxx-xxxxxxx-xxxxxxx
      roles:
       - role: minio
         vars:
           storage:
             version: 2020-11-12T22-33-34Z
             access_key: xxxxx-xxxxx-xxxxx
             secret_key: xxxxx-xxxxx-xxxxx
             data: /var/lib/minio/data
                   
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org