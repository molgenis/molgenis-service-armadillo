# Armadillo service

## Development
To test the deployment we are Vagrant to deploy the ansible playbook locally oin your machine.
If you navigate to the `deployment/ansible` directory you can execute `vagrant up` and the VM will start 
with the necessary services.

The vagrant box will bind on port 80 to the host. If you add this block to the `etc/hosts` file the domains 
in Apache HTTPD will resolve.

```
# To allow vagrant httpd to bind to the internal domains
127.0.0.1 armadillo-storage.internal
127.0.0.1 armadillo.internal
# End section
``` 

### Linting
Using ansible-lint we can check the playbook for syntax errors. We implemented the syntax highlighting in the CI-pipeline.

To install the lint tool locally, you can use brew on the Mac.

`brew install ansible-lint`

To lint your playbook execute:

`ansible-lint `

### Using Galaxy
You can build your collection using:

`ansible-galaxy collection build`

Publish your collection using

`ansible-galaxy collection publish *.tar.gz`

## Production
The playbook should at least contain this template:

```yaml
---
- hosts: all
  become: true
  become_user: root
  gather_facts: true
  vars:
    ci: false
    minio:
      access_key: xxxxxxxx
      secret_key: xxxxxxxx
      port: 9000
      host: http://localhost
    oauth:
      issuer_uri: https://auth.molgenis.org
      discovery_path: /.well-known/openid-configuration
      client_id: xxxxxxx-xxxxxx-xxxxxxx

  roles:
    - role: molgenis.armadillo.minio
      vars:
        access_key: "{{ minio.access_key }}"
        secret_key: "{{ minio.secret_key }}"
    - role: molgenis.armadillo.podman
    - role: molgenis.armadillo.httpd
      vars:
        enabled: true
        ssl: 
          enabled: true
          paths:
            server_crt: /tmp/server.crt
            private_key: /tmp/server.crt
            chain_crt: /tmp/chain.crt
        hostnames:
          armadillo: armadillo.internal
          storage: armadillo-storage.internal
        ports:
          armadillo: 8080
          storage: "{{ minio.port }}"
    - role: molgenis.armadillo.rserver
      vars: 
        debug: true
    - role: molgenis.armadillo.armadillo
      vars:
        version: 0.0.15
        storage:
          access_key: "{{ minio.access_key }}"
          secret_key: "{{ minio.secret_key }}"
          host: "{{ minio.host }}"
          port: "{{ minio.port }}"
```

### SSL certificates
We urge you to use SSL certificates for production. The port 80 exposure is ONLY meant for development.
We assume you have a wildcard certificate for both subdomains:
- armadillo-storage.exmaple.org
- armadillo.example.org

You need to specify the paths to three files.
- certificate.crt (public certificate)
- private.key (private key bound to the public certificate)
- chain.crt (the certificate chain)

### Authentication server
At this moment we use [FusionAuth](https://fusionauth.io). The configuration can be tweaked by updating the `oauth` properties.

You can choose another implementation as well, [KeyCloak](https://keycloak.io) for example.


