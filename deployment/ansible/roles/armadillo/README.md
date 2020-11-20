Armadillo
=========
This installs the Armadillo service on your system. It installs Java and creates a systemd service wrapper around the application.

[![Ansible Galaxy](https://img.shields.io/badge/ansible--galaxy-armadillo-blue.svg)](https://galaxy.ansible.com/molgenis/armadillo/)

Requirements
------------
The armadillo-role is based upon a clean CentOS 8.x image. It's dependencies are described below.

Role Variables
--------------
| Variable              | Required | Default                           | Choices  | Comments                                                                                  |
|-----------------------|----------|-----------------------------------|----------|-------------------------------------------------------------------------------------------|
| storage.version       | yes      | 2020-11-12T22-33-34Z              | na       | Version of the Minio service. There are monthly releases so you need to upgrade regularly |
| storage.access_key    | yes      | xxxxxx-xxxxxxx-xxxxxxx            | na       | The access key to access Minio API and webinterface                                       |
| storage.secret_key    | yes      | xxxxxx-xxxxxxx-xxxxxxx            | na       | The secret key to access Minio API and webinterface                                       |
| storage.data          | yes      | /var/lib/minio/data               | na       | The path on the host system where the of the Minio file storage is stored                 |
| armadillo.version     | yes      | 0.0.15                            | na       | Version of the Armadillo service. Newer versions can be found on the MOLGENIS registry.   |
| oauth.issuer_uri      | yes      | https://auth.example.org          | na       | The plain url of the authentication server (can be FusionAuth or Keycloack for example    |
| oauth.client_id       | yes      | xxxxx.xxxxxxx.xxxxxxx             | na       | The client ID of the authentication server                                                |

Dependencies
------------
This is dependant on the following list of roles:
- minio
- httpd
- rserver

Example Playbook
----------------
You can include the armadillo-role by adding the yaml block below.

    - hosts: all
      vars:
        oauth: 
          issuer_uri: https://auth.example.org
          client_id: xxxxxx-xxxxxxx-xxxxxxx
      roles:
       - role: armadillo
         vars:
           armadillo:
             version: 0.0.15
           
License
-------
See LICENSE.md

Author Information
------------------
Sido Haakma (s.haakma@rug.nl)
https://molgenis.org