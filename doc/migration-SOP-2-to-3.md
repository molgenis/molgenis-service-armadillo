# Instruction for migration a 2.x Armadillo to 3.x
The release of Armadillo 3.0 introduces some breaking changes. These instructions will guide you 
through the steps needed to migrate your old Armadillo service to the new version.



## Configuration
This section is about changes in the `application.yml` configuration file. For a full
example you can look at [the application.yml in the code](/armadillo/src/main/resources/application.yml).

### OIDC
Besides accepting JWTs from a trusted authentication provider, you can now enable OIDC authentication.
This will make it possible for admins to log in with their institute account in the UI.



### Armadillo Settings
The `datashield` parameter has been renamed to `armadillo`, and some new settings have been
introduced:

```
armadillo:
  oidc-permission-enabled: false
  docker-management-enabled: true
  oidc-admin-user: user@yourinstitute.org
```

These settings are explained in more detail below.

#### OIDC


#### Docker Management


### Profiles
Profiles can now be created and managed at runtime in the UI or via the profiles API. However,
it is still possible to configure one or more profiles, which will be created for you when
the application starts.

Before 3.0, profiles and R environments had to be defined separately:

```
rserve:
  environments:
    - name: default
      host: localhost
      port: 6311

datashield:
  profiles:
    - name: default
      environment: default
      whitelist:
        - dsBase
      options:
        datashield:
          seed: 342325352
```

This is no longer the case. You now have to configure everything inside a profile:

```
armadillo:
  profiles:
    - name: default
      image: datashield/armadillo-rserver
      host: localhost
      port: 6311
      whitelist:
        - dsBase
      options:
        datashield:
          seed: 342325352
```

Note that the root `datashield` property has been renamed to `armadillo`. 

Also note the `image` property: this one is optional and only necessary when you have enabled 
[Docker Management](#docker-management). The image should be an image available on DockerHub. 

### Storage
You can now choose where Armadillo should store data: on a MinIO server (like before) or on the local file system.

If you want to keep using MinIO, you don't need to change anything. The old configuration will still work:

```
minio:
  url: http://localhost
  port: 9000
  access-key: <access-key>
  secret-key: <secret-key>
```

To use the local file system, set the following parameter:

```
storage:
  root-dir: <path_to_data_folder>
```

MinIO has precedence over local file storage, so keep that in mind when you have both configured.

## Migration scripts

